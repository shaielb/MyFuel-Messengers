package messengers.cormessengers;

import java.util.Collection;
import java.util.List;

import cor.link.node.Node;
import db.IDbComponent;
import db.interfaces.IEntity;
import messages.Message;
import messages.QueryContainer;

public class DbRemoveNode extends Node<Message> {

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
			Collection<IEntity> entities = message.getRequest().getEntities();
			if (entities != null && entities.size() > 0) {
				for (IEntity entity : entities) {
					_dbComponent.remove(entity);
				}
				message.getResponse().setEntities(entities);
			}
			List<QueryContainer> queryContainers = message.getRequest().getQueryContainers();
			if (queryContainers != null && queryContainers.size() > 0) {
				_dbComponent.remove(queryContainers);
			}
			message.getResponse().setPassed(true);
			message.getResponse().setDescription("Entities Were Removed From Db");
		} catch (Exception e) {
			e.printStackTrace();
			message.getResponse().setPassed(false);
			message.getResponse().setDescription(e.getMessage());
			return false;
		}
		return true;
	}
}
