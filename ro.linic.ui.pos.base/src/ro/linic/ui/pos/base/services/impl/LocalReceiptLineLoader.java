package ro.linic.ui.pos.base.services.impl;

import static ro.linic.util.commons.PresentationUtils.NEWLINE;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import ro.linic.ui.base.services.LocalDatabase;
import ro.linic.ui.pos.base.model.ReceiptLine;
import ro.linic.ui.pos.base.preferences.PreferenceKey;
import ro.linic.ui.pos.base.services.ReceiptLineLoader;

@Component
public class LocalReceiptLineLoader implements ReceiptLineLoader {
	private final static ILog log = ILog.of(LocalReceiptLineLoader.class);
	
	@Reference private LocalDatabase localDatabase;
	
	@Override
	public List<ReceiptLine> findAll() {
		final IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(FrameworkUtil.getBundle(getClass()).getSymbolicName());
		final String dbName = node.get(PreferenceKey.LOCAL_DB_NAME, PreferenceKey.LOCAL_DB_NAME_DEF);
		final ReadWriteLock dbLock = localDatabase.getLock(dbName);
		
		List<ReceiptLine> result = new ArrayList<>();
		final StringBuilder querySb = new StringBuilder();
		querySb.append("SELECT ").append(NEWLINE)
		.append(SQLiteHelper.receiptLineColumns())
		.append("FROM "+ReceiptLine.class.getSimpleName());
		
		dbLock.readLock().lock();
		try (Statement stmt = localDatabase.getConnection(dbName).createStatement();
				ResultSet rs = stmt.executeQuery(querySb.toString())) {
			result = SQLiteHelper.readReceiptLines(rs);
		} catch (final SQLException e) {
			log.error(e.getMessage(), e);
		} finally {
			dbLock.readLock().unlock();
		}
		
		return result;
	}

	@Override
	public List<ReceiptLine> findByIdIn(final List<Long> ids) {
		final IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(FrameworkUtil.getBundle(getClass()).getSymbolicName());
		final String dbName = node.get(PreferenceKey.LOCAL_DB_NAME, PreferenceKey.LOCAL_DB_NAME_DEF);
		final ReadWriteLock dbLock = localDatabase.getLock(dbName);
		
		List<ReceiptLine> result = new ArrayList<>();
		final StringBuilder querySb = new StringBuilder();
		querySb.append("SELECT ").append(NEWLINE)
		.append(SQLiteHelper.receiptLineColumns())
		.append("FROM "+ReceiptLine.class.getSimpleName()).append(NEWLINE)
		.append("WHERE ").append(NEWLINE)
		.append(ReceiptLine.ID_FIELD).append(" IN (?)");
		
		dbLock.readLock().lock();
		try (PreparedStatement stmt = localDatabase.getConnection(dbName).prepareStatement(querySb.toString())) {
			stmt.setObject(1, ids);
			final ResultSet rs = stmt.executeQuery();
			result = SQLiteHelper.readReceiptLines(rs);
		} catch (final SQLException e) {
			log.error(e.getMessage(), e);
		} finally {
			dbLock.readLock().unlock();
		}
		
		return result;
	}
}
