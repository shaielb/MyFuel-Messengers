package messengers.cormessengers;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

import cor.link.node.Node;
import db.IDbComponent;
import db.entity.CustomersCharactirizationView;
import db.interfaces.IEntity;
import messages.Message;
import messages.QueryContainer;
import messages.Request;

public class DbCharactirizationNode extends Node<Message> {

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
	 *
	 */
	@Override
	public boolean execute(Message message) {
		try {
			Request request = message.getRequest();
			List<QueryContainer> queryContainers = request.getQueryContainers();
			QueryContainer queryContainer = queryContainers.get(0);
			
			Timestamp startDate = queryContainer.getStartTime();
			Timestamp endDate = queryContainer.getEndTime();
			
			Collection<IEntity> entities = _dbComponent.runFreeQuery(CustomersCharactirizationView.class, 
					"select c.customer_id id, count(c.customer_id) purchases_count, sum(fp.amount) total_amount, min(fp.date_time) first_purchase, max(fp.date_time) last_purchase, fce.company_name_key company_name " + 
					"from (select * from fueling_purchase where date_time between '" + startDate + "' and '" + endDate + "') fp inner join customer c inner join station s inner join fuel_company_enum fce " + 
					"where fp.customer_fk = c.id and fp.station_fk = s.id and s.fuel_company_enum_fk = fce.id " + 
					"group by id, fce.company_name_key;");
			message.getResponse().setPassed(true);
			message.getResponse().setEntities(entities);
		} catch (Exception e) {
			e.printStackTrace();
			message.getResponse().setPassed(false);
			message.getResponse().setDescription(e.getMessage());
			return false;
		}
		return true;
	}
}
