package mytown.datasource.types;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import mytown.MyTown;
import mytown.datasource.MyTownDatasource;
import mytown.entities.Nation;
import mytown.entities.Resident;
import mytown.entities.Town;
import mytown.entities.TownBlock;
import mytown.entities.TownPlot;
import net.minecraftforge.common.Configuration;
import net.minecraftforge.common.Property;

import com.google.common.collect.Lists;

// TODO Add logging

/**
 * Base class for all SQL based datasources
 * 
 * @author Joe Goett
 */
public abstract class MyTownDatasource_SQL extends MyTownDatasource {
	/**
	 * Used to determine how to auto increment. MySQL and SQLite uses different names
	 */
	protected static String autoIncrement = "AUTO_INCREMENT";

	protected Connection conn;
	protected Object lock = new Object();
	protected String prefix = "";
	protected String configCat = "database";

	// //////////////////////////////////////
	// Helpers
	// //////////////////////////////////////

	/**
	 * Returns a prepared statement using the given sql
	 * 
	 * @param sql
	 * @return
	 * @throws Exception
	 */
	protected PreparedStatement prepare(String sql) throws Exception {
		return prepare(sql, false);
	}

	/**
	 * Returns a PreparedStatement using the given sql
	 * 
	 * @param sql
	 * @param returnGenerationKeys
	 * @return
	 * @throws Exception
	 */
	protected PreparedStatement prepare(String sql, boolean returnGenerationKeys) throws Exception {
		if (conn == null) {
			throw new SQLException("No SQL Connection");
		}
		PreparedStatement statement = conn.prepareStatement(sql, returnGenerationKeys ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS);

		return statement;
	}

	// //////////////////////////////////////
	// Implementation
	// //////////////////////////////////////

	@Override
	protected void doConfig(Configuration config) {
		Property prop;

		prop = config.get(configCat, "Prefix", "");
		prop.comment = "The prefix of each of the tables. <prefix>tablename";
		prefix = prop.getString();
	}

	@Override
	public void loadResidents() throws Exception {
		synchronized (lock) {
			ResultSet set = null;
			PreparedStatement statement = prepare("SELECT * FROM " + prefix + "Residents");
			set = statement.executeQuery();

			while (set.next()) {
				addResident(new Resident(set.getString("Name")));
			}
		}
	}

	@Override
	public void loadNations() throws Exception {
		synchronized (lock) {
			ResultSet set = null;
			PreparedStatement statement = prepare("SELECT * FROM " + prefix + "Nations");
			set = statement.executeQuery();

			while (set.next()) {
				addNation(new Nation(set.getInt("Id"), set.getString("Name"), set.getInt("ExtraBlocks")));
			}
		}
	}

	@Override
	public void loadTowns() throws Exception {
		synchronized (lock) {
			ResultSet set = null;
			PreparedStatement statement = prepare("SELECT * FROM " + prefix + "Towns");
			set = statement.executeQuery();

			while (set.next()) {
				Town town = new Town(set.getInt("Id"), set.getString("Name"), set.getInt("ExtraBlocks"));
				addTown(town);
				loadTownBlocks(town);
			}
		}
	}

	@Override
	public void loadTownBlocks(Town town) throws Exception {
		synchronized (lock) {
			if (town == null)
				return;

			ResultSet set = null;
			PreparedStatement statement = prepare("SELECT * FROM " + prefix + "TownBlocks WHERE TownId=?");
			statement.setInt(1, town.getId());
			set = statement.executeQuery();

			while (set.next()) {
				TownBlock block = new TownBlock(set.getInt("Id"), town, set.getInt("X"), set.getInt("Z"), set.getInt("Dim"));
				town.addTownBlock(block);
				addTownBlock(block);
			}
		}
	}

	@Override
	public void loadTownPlots(Town town) throws Exception {
		synchronized (lock) {
			if (town == null)
				return;

			ResultSet set = null;
			PreparedStatement statement = prepare("SELECT * FROM " + prefix + "TownPlots WHERE TownId=?");
			statement.setInt(1, town.getId());
			set = statement.executeQuery();

			while (set.next()) {
				TownPlot plot = new TownPlot(town, set.getInt("X1"), set.getInt("Y1"), set.getInt("Z1"), set.getInt("X2"), set.getInt("Y2"), set.getInt("Z2"));
				plots.add(plot);
			}
		}
	}

	@Override
	public void updateTown(Town town) throws Exception {
		synchronized (lock) {
			PreparedStatement statement = prepare("UPDATE " + prefix + "Towns SET Name=?,ExtraBlocks=? WHERE Id=?", true);
			statement.setString(1, town.getName());
			statement.setInt(2, town.getExtraBlocks());
			statement.setInt(3, town.getId());
			statement.executeUpdate();
		}
	}

	@Override
	public void updateResident(Resident resident) throws Exception {
		synchronized (lock) {
			PreparedStatement statement = prepare("UPDATE " + prefix + "Residents SET UUID=? WHERE UUID=?", true);
			statement.setString(1, resident.getUUID());
			statement.executeUpdate();
		}
	}

	@Override
	public void updateNation(Nation nation) throws Exception {
		synchronized (lock) {
			PreparedStatement statement = prepare("UPDATE " + prefix + "Nations SET Name=?,ExtraBlocks=? WHERE Id=?", true);
			statement.setString(1, nation.getName());
			statement.setInt(2, nation.getExtraBlocksPerTown());
			statement.setInt(3, nation.getId());
			statement.executeUpdate();
		}
	}

	@Override
	public void updateTownBlock(TownBlock block) throws Exception {
		synchronized (lock) {
			PreparedStatement statement = prepare("UPDATE " + prefix + "TownBlocks SET X=?,Z=?,Dim=? WHERE Id=?", true);
			statement.setInt(1, block.getX());
			statement.setInt(2, block.getZ());
			statement.setInt(3, block.getDim());
			statement.setInt(4, block.getId());
			statement.executeUpdate();
		}
	}

	@Override
	public void updateTownPlot(TownPlot plot) throws Exception {
		synchronized (lock) {
			PreparedStatement statement = prepare("UPDATE " + prefix + "TownPlots SET X1=?, Y1=?, Z1=?, X2=?, Y2=?, Z2=?, Dim=?, TownId=?, Owner=?", true);
			statement.setInt(1, plot.x1);
			statement.setInt(2, plot.y1);
			statement.setInt(3, plot.z1);
			statement.setInt(4, plot.x2);
			statement.setInt(5, plot.y2);
			statement.setInt(6, plot.z2);
			statement.setInt(7, plot.dim);
			statement.setInt(8, plot.getTownId());
			statement.setString(9, plot.getOwnerUUID());
			statement.executeUpdate();
		}
	}

	@Override
	public void insertTown(Town town) throws Exception {
		synchronized (lock) {
			addTown(town);
			PreparedStatement statement = prepare("INSERT INTO " + prefix + "Towns (Name,ExtraBlocks) VALUES (?,?)", true);
			statement.setString(1, town.getName());
			statement.setInt(2, town.getExtraBlocks());
			statement.executeUpdate();

			ResultSet rs = statement.getGeneratedKeys();
			if (!rs.next()) {
				throw new RuntimeException("Id wasn't returned for new town " + town.getName());
			}

			town.setId(rs.getInt(1));
		}
	}

	@Override
	public void insertResident(Resident resident) throws Exception {
		synchronized (lock) {
			addResident(resident);
			PreparedStatement statement = prepare("INSERT INTO " + prefix + "Residents (Name) VALUES (?)", true);
			statement.setString(1, resident.getUUID());
			statement.executeUpdate();
		}
	}

	@Override
	public void insertNation(Nation nation) throws Exception {
		synchronized (lock) {
			addNation(nation);
			PreparedStatement statement = prepare("INSERT INTO " + prefix + "Nations (Name,ExtraBlocks) VALUES (?,?)", true);
			statement.setString(1, nation.getName());
			statement.setInt(2, nation.getExtraBlocksPerTown());
			statement.executeUpdate();

			ResultSet rs = statement.getGeneratedKeys();
			if (!rs.next()) {
				throw new RuntimeException("Id wasn't returned for new nation " + nation.getName());
			}

			nation.setId(rs.getInt(1));
		}
	}

	@Override
	public void insertTownBlock(TownBlock townBlock) throws Exception {
		synchronized (lock) {
			addTownBlock(townBlock);
			PreparedStatement statement = prepare("INSERT INTO " + prefix + "TownBlocks (X, Z, Dim, TownId) VALUES (?, ?, ?, ?)", true);
			statement.setInt(1, townBlock.getX());
			statement.setInt(2, townBlock.getZ());
			statement.setInt(3, townBlock.getDim());
			statement.setInt(4, townBlock.getTownID());
			statement.executeUpdate();

			ResultSet rs = statement.getGeneratedKeys();
			if (!rs.next()) {
				throw new RuntimeException("Id wasn't returned for new TownBlock " + townBlock.getKey());
			}

			townBlock.setId(rs.getInt(1));
		}
	}

	@Override
	public void insertTownPlot(TownPlot townPlot) throws Exception {
		synchronized (lock) {
			addTownPlot(townPlot);
			PreparedStatement statement = prepare("INSERT INTO " + prefix + "TownPlots (X1, Y1, Z1, X2, Y2, Z2, Dim, TownId, Owner) VALUES(?,?,?,?,?,?,?,?,?)", true);
			statement.setInt(1, townPlot.x1);
			statement.setInt(2, townPlot.y1);
			statement.setInt(3, townPlot.z1);
			statement.setInt(4, townPlot.x2);
			statement.setInt(5, townPlot.y2);
			statement.setInt(6, townPlot.z2);
			statement.setInt(7, townPlot.dim);
			statement.setInt(8, townPlot.getTownId());
			statement.setString(9, townPlot.getOwnerUUID());
			statement.executeUpdate();

			ResultSet rs = statement.getGeneratedKeys();
			if (!rs.next()) {
				throw new RuntimeException("Id wasn't returned for new TownPlot");
			}

			townPlot.id = rs.getInt(1);
		}
	}

	@Override
	public void linkResidentToTown(Resident resident, Town town, Resident.Rank rank) throws Exception {
		synchronized (lock) {
			resident.addTown(town);
			town.addResident(resident, rank);

			PreparedStatement statement = prepare("INSERT INTO " + prefix + "ResidentsToTowns (TownId, Owner, Rank) VALUES (?, ?, ?)", true);
			statement.setInt(1, town.getId());
			statement.setString(2, resident.getUUID());
			statement.setString(3, rank.toString());
			statement.executeUpdate();
		}
	}

	@Override
	public void linkTownToNation(Town town, Nation nation, Nation.Rank rank) throws Exception {
		synchronized (lock) {
			town.addNation(nation);
			nation.addTown(town, rank);

			PreparedStatement statement = prepare("INSERT INTO " + prefix + "TownsToNations (TownId, NationId, Rank) VALUES(?, ?, ?)", true);
			statement.setInt(1, town.getId());
			statement.setInt(2, nation.getId());
			statement.setString(3, rank.toString());
			statement.executeQuery();
		}
	}

	@Override
	public void dump() throws Exception {
		// TODO Finish dump()
	}

	@Override
	public void save() throws Exception {
	}

	@Override
	public void disconnect() throws Exception {
		if (conn == null)
			return;
		if (!conn.getAutoCommit())
			conn.commit();
		conn.close();
	}

	// //////////////////////////////////////
	// Update System
	// //////////////////////////////////////

	/**
	 * Holds an SQL statement to be run to update the tables in the DB
	 * 
	 * @author Joe Goett
	 */
	protected class DBUpdate {
		/**
		 * Formatted mm.dd.yyyy.e where e increments by 1 for every update released on the same date
		 */
		public String id;
		public String code;
		public String sql;

		public DBUpdate(String id, String code, String sql) {
			this.id = id;
			this.code = code;
			this.sql = sql;
		}
	}

	protected List<DBUpdate> updates = new ArrayList<DBUpdate>();

	/**
	 * Create all the new updates
	 */
	protected void setUpdates() {
		updates.add(new DBUpdate("03.08.2014.1", "Add Updates Table", "CREATE TABLE IF NOT EXISTS " + prefix + "Updates (Id varchar(50) NOT NULL, Code varchar(50) NOT NULL, PRIMARY KEY(Id));"));
		updates.add(new DBUpdate("03.08.2014.2", "Add Towns Table", "CREATE TABLE IF NOT EXISTS " + prefix + "Towns (Id int NOT NULL " + autoIncrement + ", Name varchar(50) NOT NULL, ExtraBlocks int NOT NULL, PRIMARY KEY (Id));"));
		updates.add(new DBUpdate("03.08.2014.3", "Add Residents Table", "CREATE TABLE IF NOT EXISTS " + prefix + "Residents (UUID varchar(255) NOT NULL, IsNPC boolean DEFAULT false, Joined int NOT NULL, LastLogin int NOT NULL, PRIMARY KEY (UUID));")); // MC Version < 1.7 UUID is Player name. 1.7 >= UUID is Player's UUID
		updates.add(new DBUpdate("03.08.2014.4", "Add Nations Table", "CREATE TABLE IF NOT EXISTS " + prefix + "Nations (Id int NOT NULL " + autoIncrement + ", Name varchar(50) NOT NULL, ExtraBlocks int NOT NULL DEFAULT 0, PRIMARY KEY(Id));"));
		updates.add(new DBUpdate("03.08.2014.5", "Add TownBlocks Table", "CREATE TABLE IF NOT EXISTS " + prefix + "TownBlocks (Id int NOT NULL " + autoIncrement + ", X int NOT NULL, Z int NOT NULL, Dim int NOT NULL, TownId int NOT NULL, PRIMARY KEY(Id), FOREIGN KEY (TownId) REFERENCES " + prefix
				+ "Towns(Id) ON DELETE CASCADE);"));
		updates.add(new DBUpdate("03.15.2014.1", "Add TownPlots Table", "CREATE TABLE IF NOT EXISTS " + prefix + "TownPlots (Id int NOT NULL " + autoIncrement
				+ ", X1 int NOT NULL, Y1 int NOT NULL, Z1 int NOT NULL, X2 int NOT NULL, Y2 int NOT NULL, Z2 int NOT NULL, Dim int NOT NULL, TownId int NOT NULL, Owner varchar(255) DEFAULT NULL, Rank varchar(1) DEFAULT 'R', PRIMARY KEY(Id), FOREIGN KEY (TownId) REFERENCES " + prefix
				+ "Towns(Id) ON DELETE CASCADE, FOREIGN KEY (Owner) REFERENCES " + prefix + "Residents(UUID) ON DELETE SET NULL);"));
		updates.add(new DBUpdate("03.22.2014.1", "Add ResidentsToTowns Table", "CREATE TABLE IF NOT EXISTS " + prefix + "ResidentsToTowns (Id int NOT NULL " + autoIncrement + ", TownId int NOT NULL, Owner varchar(255) NOT NULL, PRIMARY KEY (Id), FOREIGN KEY (TownId) REFERENCES " + prefix
				+ "Towns(Id) ON DELETE CASCADE, FOREIGN KEY (Owner) REFERENCES " + prefix + "Residents(UUID) ON DELETE CASCADE);"));
		updates.add(new DBUpdate("03.22.2014.2", "Add TownsToNations", "CREATE TABLE IF NOT EXISTS " + prefix + "TownsToNations (Id int NOT NULL " + autoIncrement + ", TownId int NOT NULL, NationId int NOT NULL, Rank varchar(1) DEFAULT 'T', PRIMARY KEY (Id), FOREIGN KEY (TownId) REFERENCES "
				+ prefix + "Towns(Id) ON DELETE CASCADE, FOREIGN KEY (NationId) REFERENCES " + prefix + "Nations(Id) ON DELETE CASCADE);"));
	}

	/**
	 * Does the actual updates on the DB
	 * 
	 * @throws Exception
	 */
	protected void doUpdates() throws Exception {
		List<String> ids = Lists.newArrayList();
		PreparedStatement statement;
		try {
			statement = prepare("SELECT * FROM " + prefix + "Updates");
			ResultSet rs = statement.executeQuery();

			while (rs.next()) {
				ids.add(rs.getString("Id"));
			}
		} catch (Exception e) {
		} // Ignore. Just missing the updates table for now

		for (DBUpdate update : updates) {
			if (ids.contains(update.id))
				continue; // Skip updates already done

			// Update!
			MyTown.INSTANCE.datasourceLog.info("Running update %s - %s", update.id, update.code);
			statement = prepare(update.sql);
			statement.execute();

			// Insert the update key so as to not run the update again
			statement = prepare("INSERT INTO " + prefix + "Updates (Id,Code) VALUES(?,?)", true);
			statement.setString(1, update.id);
			statement.setString(2, update.code);
			statement.execute();
		}
	}
}