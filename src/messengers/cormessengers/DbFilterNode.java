package messengers.cormessengers;

import java.util.List;

import cor.link.node.Node;
import db.IDbComponent;
import db.interfaces.IEntity;
import messages.Message;
import messages.QueryContainer;
import messages.Request;

public class DbFilterNode extends Node<Message> {

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
			List<IEntity> entities = _dbComponent.filter(queryContainers);
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
