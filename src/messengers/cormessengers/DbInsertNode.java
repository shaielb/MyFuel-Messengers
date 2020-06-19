package messengers.cormessengers;

import java.util.Collection;
import java.util.List;

import annotations.Table;
import cor.link.node.Node;
import db.IDbComponent;
import db.interfaces.IEntity;
import db.interfaces.IEntityBridge;
import db.services.Services;
import messages.Message;

public class DbInsertNode extends Node<Message> {

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
				insertUpdate(entity);
			}
			message.getResponse().setEntities(entities);
			message.getResponse().setPassed(true);
			message.getResponse().setDescription("Entities Were Added To Db");
		} catch (Exception e) {
			e.printStackTrace();
			message.getResponse().setPassed(false);
			message.getResponse().setDescription(e.getMessage());
			return false;
		}
		return true;
	}
	
	/**
	 * inserting or updating all related tables
	 * @param entity
	 * @throws Exception
	 */
	/**
	 * @param entity
	 * @throws Exception
	 */
	private void insertUpdate(IEntity entity) throws Exception {
		/*String table = entity.getClass().getAnnotation(Table.class).Name();
		IEntityBridge bridge = Services.getBridge(table);
		List<IEntity> list = bridge.getForeignEntities(entity);
		for (IEntity fEntity : list) {
			insertUpdate(fEntity);
		}*/
		if (entity.getId() == null || entity.getId() == -1) {
			_dbComponent.insert(entity);
		}
		else {
			_dbComponent.update(entity);
		}
	}
}
