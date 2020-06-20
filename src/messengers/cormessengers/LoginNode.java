package messengers.cormessengers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cor.link.node.Node;
import db.IDbComponent;
import db.entity.Address;
import db.entity.Customer;
import db.entity.Employee;
import db.entity.FuelCompanyEnum;
import db.entity.Payment;
import db.entity.Person;
import db.entity.Station;
import db.entity.SystemUser;
import db.interfaces.IEntity;
import messages.Message;
import messages.QueryContainer;
import messages.Request;

public class LoginNode extends Node<Message> {

	/**
	 * 
	 */
	private static final String DbComponent = "dbComponent";

	private static final String CustomerPermission = "Customer";

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
			List<QueryContainer> systemUserContainers = request.getQueryContainers();
			List<IEntity> systemUsers = _dbComponent.filter(systemUserContainers);
			if (systemUsers == null || systemUsers.size() == 0) {
				message.getResponse().setPassed(false);
				message.getResponse().setDescription("User Name and Password Were Not Found");
				return false;
			}
			SystemUser systemUser = (SystemUser) systemUsers.get(0);
			if (systemUser.getOnlineStatus()) {
				message.getResponse().setPassed(false);
				message.getResponse().setDescription("User Is Allready Logged In");
				return false;
			}
			systemUser.setOnlineStatus(true);
			Set<IEntity> set = new HashSet<IEntity>();

			// looking for the related customers
			if (CustomerPermission.equals(systemUser.getPermission())) {
				Customer customer = collectCustomer(message, systemUser);
				if (customer == null) {
					return false;
				}
				set.add(customer);
				Person pesron = collectPerson(message, customer.getPerson());
				if (pesron == null) {
					return false;
				}
				customer.setPerson(pesron);
				set.add(pesron);

				Address address = collectAddress(message, customer.getAddress());
				if (address == null) {
					return false;
				}
				customer.setAddress(address);

				Payment payment = collectPayment(message, customer.getPayment());
				if (payment == null) {
					return false;
				}
				customer.setPayment(payment);
			}
			// looking for the related employees
			else {
				Employee employee = collectEmployee(message, systemUser);
				if (employee == null) {
					return false;
				}
				set.add(employee);
				Person pesron = collectPerson(message, employee.getPerson());
				if (pesron == null) {
					return false;
				}
				employee.setPerson(pesron);
				
				FuelCompanyEnum fuelCompanyEnum = (employee.getFuelCompanyEnum() == null) ? null : collectFuelCompany(message, employee.getFuelCompanyEnum());
				employee.setFuelCompanyEnum(fuelCompanyEnum);
				
				Station station = (employee.getStation() == null) ? null : collectStation(message, employee.getStation());
				employee.setStation(station);
				
				set.add(pesron);
			}

			// updating the system user's online status
			_dbComponent.update(systemUser);
			set.add(systemUser);
			message.getResponse().setEntities(set);
			message.getResponse().setPassed(true);
		} catch (Exception e) {
			e.printStackTrace();
			message.getResponse().setPassed(false);
			message.getResponse().setDescription(e.getMessage());
			return false;
		}
		return true;
	}

	private Employee collectEmployee(Message message, SystemUser systemUser) throws Exception {
		Employee employee = new Employee();
		QueryContainer employeeContainer = new QueryContainer();
		employee.setSystemUser(systemUser);
		employeeContainer.setQueryEntity(employee);
		employeeContainer.addQueryCondition("system_user_fk", "=");
		List<IEntity> employees = _dbComponent.filter(employeeContainer);
		if (employees == null || employees.size() == 0) {
			message.getResponse().setPassed(false);
			message.getResponse().setDescription("No Employees Found With This User Name and Password");
			return null;
		}
		Employee employeeToAttach = (Employee) employees.get(0);
		employeeToAttach.setSystemUser(systemUser);
		return employeeToAttach;
	}

	private Customer collectCustomer(Message message, SystemUser systemUser) throws Exception {
		Customer customer = new Customer();
		QueryContainer customerContainer = new QueryContainer();
		customer.setSystemUser(systemUser);
		customerContainer.setQueryEntity(customer);
		customerContainer.addQueryCondition("system_user_fk", "=");
		List<IEntity> customers = _dbComponent.filter(customerContainer);
		if (customers == null || customers.size() == 0) {
			message.getResponse().setPassed(false);
			message.getResponse().setDescription("No Customers Found With This User Name and Password");
			return null;
		}
		Customer customerToAttach = (Customer) customers.get(0);
		customerToAttach.setSystemUser(systemUser);
		return customerToAttach;
	}

	private Person collectPerson(Message message, Person person) throws Exception {
		QueryContainer personContainer = new QueryContainer();
		personContainer.addQueryCondition("id", "=");
		List<IEntity> persons;

		personContainer.setQueryEntity(person);
		persons = _dbComponent.filter(personContainer);
		if (persons == null || persons.size() == 0) {
			message.getResponse().setPassed(false);
			message.getResponse().setDescription("No Customers Found With This User Name and Password");
			return null;
		}
		return (Person) persons.get(0);
	}

	private Address collectAddress(Message message, Address address) throws Exception {
		QueryContainer addressContainer = new QueryContainer();
		addressContainer.addQueryCondition("id", "=");
		List<IEntity> addresses;

		addressContainer.setQueryEntity(address);
		addresses = _dbComponent.filter(addressContainer);
		if (addresses == null || addresses.size() == 0) {
			message.getResponse().setPassed(false);
			message.getResponse().setDescription("This Customer Has No Address Related");
			return null;
		}
		return (Address) addresses.get(0);
	}

	private Payment collectPayment(Message message, Payment payment) throws Exception {
		QueryContainer paymentContainer = new QueryContainer();
		paymentContainer.addQueryCondition("id", "=");
		List<IEntity> payments;

		paymentContainer.setQueryEntity(payment);
		payments = _dbComponent.filter(paymentContainer);
		if (payments == null || payments.size() == 0) {
			message.getResponse().setPassed(false);
			message.getResponse().setDescription("Tis Customer Has No Payment Details Related");
			return null;
		}
		return (Payment) payments.get(0);
	}
	
	private FuelCompanyEnum collectFuelCompany(Message message, FuelCompanyEnum fuelCompany) throws Exception {
		QueryContainer fuelCompanyContainer = new QueryContainer();
		fuelCompanyContainer.addQueryCondition("id", "=");
		List<IEntity> fuelCompanies;

		fuelCompanyContainer.setQueryEntity(fuelCompany);
		fuelCompanies = _dbComponent.filter(fuelCompanyContainer);
		if (fuelCompanies == null || fuelCompanies.size() == 0) {
			return null;
		}
		return (FuelCompanyEnum) fuelCompanies.get(0);
	}
	
	private Station collectStation(Message message, Station station) throws Exception {
		QueryContainer stationContainer = new QueryContainer();
		stationContainer.addQueryCondition("id", "=");
		List<IEntity> stations;

		stationContainer.setQueryEntity(station);
		stations = _dbComponent.filter(stationContainer);
		if (stations == null || stations.size() == 0) {
			return null;
		}
		return (Station) stations.get(0);
	}
}
