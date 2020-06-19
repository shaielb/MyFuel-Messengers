package messengers.cormessengers;

import java.util.Collection;

import cor.link.node.Node;
import db.IDbComponent;
import db.interfaces.IEntity;
import messages.Message;

public class DbUpdateNode extends Node<Message> {

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
			for (IEntity entity : entities) {
				_dbComponent.update(entity);
			}
			message.getResponse().setEntities(entities);
			message.getResponse().setPassed(true);
			message.getResponse().setDescription("Entities Were Updated In Db");
		} catch (Exception e) {
			e.printStackTrace();
			message.getResponse().setPassed(false);
			message.getResponse().setDescription(e.getMessage());
			return false;
		}
		return true;
	}
}
