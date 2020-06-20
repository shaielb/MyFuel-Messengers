package messengers.cormessengers;

import java.sql.Timestamp;
import java.util.List;

import cor.link.node.Node;
import db.IDbComponent;
import db.entity.Car;
import db.entity.Customer;
import db.entity.FastfuelingPricingModelView;
import db.entity.FuelEnum;
import db.entity.FuelingPurchase;
import db.entity.SpecialSaleView;
import db.entity.SpecialSalesEnum;
import db.entity.Station;
import db.entity.StationSupplyOrder;
import db.entity.StationsFuel;
import db.interfaces.IEntity;
import messages.Message;
import messages.QueryContainer;
import messages.Request;

public class FastFuelingNode extends Node<Message> {

	private static String Standard = "Standard";
	private static String MonthlyS = "MonthlyS";
	private static String MonthlyM = "MonthlyM";
	private static String MonthlyFS = "MonthlyFS";

	private static String Single = "Single";
	private static String Multi = "Multi";
	private static String None = "None";

	private static String Cash = "Cash";
	private static String Credit = "Credit";

	private static String WaitingForApproval = "Waiting For Approval";

	/**
	 * 
	 */
	private static final String DbComponent = "dbComponent";

	/**
	 * 
	 */
	private IDbComponent _dbComponent;

	/**
	 *
	 */
	@Override
	public void initialize() throws Exception {
		_dbComponent = (IDbComponent) _iocContainer.resolve(DbComponent);
	}

	/**
	 * @Message the message containing the desired information to carry out the action
	 * @return false if you want to cut the chain sequence
	 */
	@Override
	public boolean execute(Message message) {
		try {
			Request request = message.getRequest();
			List<QueryContainer> queryContainers = request.getQueryContainers();
			FastfuelingPricingModelView ffpmv = null;
			FuelingPurchase fuelingPurchase = null;
			for (QueryContainer qc: queryContainers) {
				if (qc.getQueryEntity() instanceof FastfuelingPricingModelView) {
					ffpmv = (FastfuelingPricingModelView) qc.getQueryEntity();
				}
				else if (qc.getQueryEntity() instanceof FuelingPurchase) {
					fuelingPurchase = (FuelingPurchase) qc.getQueryEntity();
				}
			}

			Double amount = fuelingPurchase.getAmount();

			Timestamp currTime = new Timestamp(System.currentTimeMillis());

			Station station = new Station();
			station.setStationName(fuelingPurchase.getStation().getStationName());
			QueryContainer stationQc = new QueryContainer(station);
			stationQc.addQueryCondition("station_name", "=");
			List<IEntity> stations = _dbComponent.filter(stationQc);
			Station selectedStation = (Station) stations.get(0);

			SpecialSaleView ssv = new SpecialSaleView();
			ssv.setStationName(fuelingPurchase.getStation().getStationName());

			QueryContainer qcSpecialSaleViewContainer = new QueryContainer(ssv);
			qcSpecialSaleViewContainer.addQueryCondition("station_name", "=");
			List<IEntity> ssvEntities = _dbComponent.filter(qcSpecialSaleViewContainer);
			SpecialSaleView sse = null;
			String companyName = selectedStation.getFuelCompanyEnum().getCompanyNameKey();
			for (IEntity entity : ssvEntities) {
				SpecialSaleView ssvCurr = (SpecialSaleView) entity;
				if (currTime.compareTo(ssvCurr.getStartTime()) >= 0 && currTime.compareTo(ssvCurr.getEndTime()) <= 0) {
					sse = ssvCurr;
					break;
				}
			}

			QueryContainer qcFastfuelingPricingModelViewContainer = new QueryContainer(ffpmv);
			qcFastfuelingPricingModelViewContainer.addQueryCondition("plate_number", "=");
			List<IEntity> ffEntities = _dbComponent.filter(qcFastfuelingPricingModelViewContainer);
			FastfuelingPricingModelView ffpme = null;
			for (IEntity entity : ffEntities) {
				FastfuelingPricingModelView ffpmvCurr = (FastfuelingPricingModelView) entity;
				if (ffpmvCurr.getCompanyName().equals(companyName)) {
					ffpme = ffpmvCurr;
				}
			}
			
			StationsFuel sf = new StationsFuel();
			sf.setStation(selectedStation);
			QueryContainer qcStation = new QueryContainer(sf);
			qcStation.addQueryCondition("station_fk", "=");
			List<IEntity> stationsF = _dbComponent.filter(qcStation);
			for (IEntity entity : stationsF) {
				if (((StationsFuel)entity).getFuelEnum().getFuelTypeKey().equals(ffpme.getFuelType())) {
					sf = (StationsFuel)entity;
					break;
				}
			}
			if (sf.getCurrentAmount() < amount) {
				message.getResponse().setPassed(false);
				message.getResponse().setDescription("Ther Is Not Enough Fuel Left In The Selected Station");
				return false;
			}

			FuelEnum fe = new FuelEnum();
			FuelingPurchase fpToInsert = new FuelingPurchase();

			if (ffpme != null) {
				Double pmcd = ffpme.getPricingModelCompanyDiscount();
				Double maxPrice = ffpme.getMaxPrice();
				String modelType = ffpme.getModelType();
				Double price = 0d;
				if (Standard.equals(modelType)) {
					price = amount * maxPrice;
				}
				else if (MonthlyS.equals(modelType)) {
					price = (maxPrice * pmcd) * amount;
				}
				else if (MonthlyM.equals(modelType)) {
					price = ((maxPrice * pmcd) * amount) * 0.9;
				}
				else if (MonthlyFS.equals(modelType)) {
					price = ((maxPrice * pmcd) * amount) * 0.97;
				}

				// subscription plan discount (using ffpme.getPlanType())
				price *= ffpme.getSubscriptionPlanDiscount();

				SpecialSalesEnum ssEnum = null;
				if (sse != null) {
					ssEnum = new SpecialSalesEnum();
					if (amount > sse.getAmountLimitation()) {
						price *= sse.getDiscout();
						ssEnum.setId(sse.getSpecialSaleId());
					}
				}

				fpToInsert.setDateTime(currTime);
				fpToInsert.setAmount(amount);
				fpToInsert.setPricePerLiter(price / amount);
				fpToInsert.setTotalPrice(price);
				Car car = new Car();
				// the view id is the car id
				car.setId(ffpme.getId());
				fpToInsert.setCar(car);
				fpToInsert.setSpecialSalesEnum(ssEnum);
				fpToInsert.setStation(selectedStation);
				fe.setId(ffpme.getFuelDbId());
				fpToInsert.setFuelEnum(fe);
				Customer customer = new Customer();
				customer.setId(ffpme.getCustomerDbId());
				fpToInsert.setCustomer(customer);
				fpToInsert.setPriceType(ffpme.getIsCash() ? Cash : Credit);

				_dbComponent.insert(fpToInsert);
			}

			Double newAmount = sf.getCurrentAmount() - amount;
			sf.setCurrentAmount(newAmount);
			_dbComponent.update(sf);

			if (newAmount < sf.getFuelLowLevel()) {
				StationSupplyOrder order = new StationSupplyOrder();
				order.setOrderDate(currTime);
				Double amountToOrder = sf.getMaxAmount() - newAmount;
				order.setAmount(amountToOrder);
				order.setOrderStatus(WaitingForApproval);
				order.setTotalPrice(ffpme.getSupplierPrice() * amountToOrder);
				order.setFuelEnum(fe);
				order.setStation(selectedStation);
				_dbComponent.insert(order);
			}

			message.getResponse().addEntity(fpToInsert);
			message.getResponse().addEntity(ffpme);
			message.getResponse().setPassed(true);
		} catch (Exception e) {
			e.printStackTrace();
			message.getResponse().setPassed(false);
			message.getResponse().setDescription(e.getMessage());
			return false;
		}
		return true;
	}
}
