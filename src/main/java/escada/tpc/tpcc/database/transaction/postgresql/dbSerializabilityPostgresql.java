/*
 * Copyright 2013 Universidade do Minho
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software   distributed under the License is distributed on an "AS IS" BASIS,   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and limitations under the License.
 */

package escada.tpc.tpcc.database.transaction.postgresql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;

import org.apache.log4j.Logger;

import escada.tpc.tpcc.database.transaction.dbTPCCDatabase;

/**
 * It is an interface to a postgreSQL, which based is based on the the
 * distributions of the TPC-C.
 */
public class dbSerializabilityPostgresql extends dbTPCCDatabase {

	private static Logger logger = Logger.getLogger(dbPostgresql.class);

	protected HashSet NewOrderDB(Properties obj, Connection con)
			throws java.sql.SQLException {

		boolean resubmit = Boolean.parseBoolean((String) obj.get("resubmit"));
		HashSet dbtrace = new HashSet();

		while (true) {
			PreparedStatement statement = null;
			ResultSet rs = null;
			String cursor = null;
			StringBuffer str = new StringBuffer();
			try {

				Date NetStartTime = new java.util.Date();

				str.append(" select * from handleSelect('");
				if (obj.get("wid") != null && obj.get("did") != null)
					str.append(" select * from district where d_w_id = "
							+ obj.get("wid") + " and d_id = " + obj.get("did")
							+ ";");

				statement = con
						.prepareCall("select tpcc_neworder (?,?,?,?,?,?,?,?)");

				statement.setInt(1, Integer.parseInt((String) obj.get("wid")));
				statement.setInt(2, Integer.parseInt((String) obj.get("did")));
				statement.setInt(3, Integer.parseInt((String) obj.get("cid")));
				statement.setInt(4, Integer.parseInt((String) obj.get("qtd")));
				statement.setInt(5, Integer.parseInt((String) obj
						.get("localwid")));

				int icont = 0;
				int qtd = Integer.parseInt((String) obj.get("qtd"));
				StringBuffer iid = new StringBuffer();
				StringBuffer wid = new StringBuffer();
				StringBuffer qtdi = new StringBuffer();
				while (icont < qtd) {
					if (obj.get("iid" + icont) != null) {
						str.append(" select * from item where i_id = "
								+ obj.get("iid" + icont) + ";");
						if (obj.get("supwid" + icont) != null)
							str.append(" select * from stock where s_i_id = "
									+ obj.get("iid" + icont) + " and s_w_id = "
									+ obj.get("supwid" + icont) + ";");
					}

					iid.append((String) obj.get("iid" + icont));
					iid.append(",");
					wid.append((String) obj.get("supwid" + icont));
					wid.append(",");
					qtdi.append((String) obj.get("qtdi" + icont));
					qtdi.append(",");
					icont++;
				}

				if (obj.get("cid") != null && obj.get("wid") != null
						&& obj.get("did") != null)
					str.append("select * from customer where c_id = "
							+ obj.get("cid") + " and c_w_id = "
							+ obj.get("wid") + " and c_d_id = "
							+ obj.get("did") + ";");
				if (obj.get("wid") != null)
					str.append("select * from warehouse where w_id = "
							+ obj.get("wid") + ";");
				str.append("');");

				statement.setString(6, iid.toString());
				statement.setString(7, wid.toString());
				statement.setString(8, qtdi.toString());

				rs = statement.executeQuery();

				if (rs.next()) {
					cursor = (String) rs.getString(1);
				}
				rs.close();
				rs = null;
				statement.close();
				statement = null;
				statement = con.prepareStatement("fetch all in \"" + cursor
						+ "\"");
				rs = statement.executeQuery();

				while (rs.next()) {
					dbtrace.add(rs.getString(1));
				}
				rs.close();
				rs = null;
				statement.close();
				statement = null;

				statement = con.prepareStatement(str.toString());
				rs = statement.executeQuery();
				Date NetFinishTime = new java.util.Date();

				processLog(NetStartTime, NetFinishTime, "processing", "w",
						"tx neworder");

			} catch (java.sql.SQLException sqlex) {
				logger.warn("NewOrder - SQL Exception " + sqlex.getMessage());
				if ((sqlex.getMessage().indexOf("serialize") != -1)
						|| (sqlex.getMessage().indexOf("deadlock") != -1)) {
					RollbackTransaction(con, sqlex, "tx neworder", "w");
					if (resubmit) {
						InitTransaction(con, "tx neworder", "w");
						continue;
					} else {
						throw sqlex;
					}
				} else {
					RollbackTransaction(con, sqlex, "tx neworder", "w");
					throw sqlex;
				}
			} catch (java.lang.Exception ex) {
				logger.fatal("Unexpected error. Something bad happend");
				ex.printStackTrace(System.err);
				System.exit(-1);
			} finally {
				if (rs != null) {
					rs.close();
				}
				if (statement != null) {
					statement.close();
				}
			}
			break;
		}
		return (dbtrace);
	}

	protected HashSet DeliveryDB(Properties obj, Connection con)
			throws java.sql.SQLException {

		boolean resubmit = Boolean.parseBoolean((String) obj.get("resubmit"));
		HashSet dbtrace = new HashSet();
		StringBuffer str = new StringBuffer();
		while (true) {

			PreparedStatement statement = null;
			ResultSet rs = null;
			String cursor = null;

			try {
				Date NetStartTime = new java.util.Date();

				str.append("select * from handleSelect('");

				for (int _d_id = 0; _d_id < 10; _d_id++) {

					if (obj.get("wid") != null) {
						str.append("select * from new_order where no_w_id = "
								+ obj.get("wid") + " and no_d_id = " + _d_id
								+ ";");
						statement = con
								.prepareStatement("select * from new_order where no_w_id = "
										+ obj.get("wid")
										+ " and no_d_id = "
										+ _d_id
										+ " order by no_o_id asc limit 1;");

						rs = statement.executeQuery();
						int _o_id = 0;
						if (rs.next()) {
							_o_id = rs.getInt("no_o_id");

							str
									.append("select * from new_order where no_w_id = "
											+ obj.get("wid")
											+ " and no_d_id = "
											+ _d_id
											+ " and no_o_id = " + _o_id + ";");

							str.append("select * from orders where o_w_id = "
									+ obj.get("wid") + " and o_d_id = " + _d_id
									+ " and o_id = " + _o_id + ";");
							statement = con
									.prepareStatement("select * from new_order where no_w_id = "
											+ obj.get("wid")
											+ " and no_d_id = "
											+ _d_id
											+ " order by no_o_id asc limit 1;");
							rs = statement
									.executeQuery("select * from orders where o_w_id = "
											+ obj.get("wid")
											+ " and o_d_id = "
											+ _d_id
											+ " and o_id = "
											+ _o_id
											+ ";");

						}

						int _c_id = 0;
						if (rs.next()) {
							_c_id = rs.getInt("o_c_id");

							str
									.append("select * from order_line where ol_w_id = "
											+ obj.get("wid")
											+ " and ol_d_id = "
											+ _d_id
											+ " and ol_o_id = " + _o_id + ";");

							str
									.append("select * from order_line where c_w_id = "
											+ obj.get("wid")
											+ " and c_d_id = "
											+ _d_id
											+ " and c_id = "
											+ _c_id
											+ ";");
						}
					}
				}
				str.append("');");
				statement = con.prepareStatement("select tpcc_delivery(?,?)");

				statement.setInt(1, Integer.parseInt((String) obj.get("wid")));
				statement.setInt(2, Integer.parseInt((String) obj.get("crid")));
				rs = statement.executeQuery();

				if (rs.next()) {
					cursor = (String) rs.getString(1);
				}
				rs.close();
				rs = null;
				statement.close();
				statement = null;
				statement = con.prepareStatement("fetch all in \"" + cursor
						+ "\"");
				rs = statement.executeQuery();

				while (rs.next()) {
					dbtrace.add(rs.getString(1));
				}
				rs.close();
				rs = null;
				statement.close();
				statement = null;
				statement = con.prepareStatement(str.toString());
				rs = statement.executeQuery();

				Date NetFinishTime = new java.util.Date();

				processLog(NetStartTime, NetFinishTime, "processing", "w",
						"tx delivery");

			} catch (java.sql.SQLException sqlex) {
				logger.warn("Delivery - SQL Exception " + sqlex.getMessage());
				if ((sqlex.getMessage().indexOf("serialize") != -1)
						|| (sqlex.getMessage().indexOf("deadlock") != -1)) {
					RollbackTransaction(con, sqlex, "tx delivery", "w");
					if (resubmit) {
						InitTransaction(con, "tx delivery", "w");
						continue;
					} else {
						throw sqlex;
					}
				} else {
					RollbackTransaction(con, sqlex, "tx delivery", "w");
					throw sqlex;
				}
			} catch (java.lang.Exception ex) {
				logger.fatal("Unexpected error. Something bad happend");
				ex.printStackTrace(System.err);
				System.exit(-1);
			} finally {
				if (rs != null) {
					rs.close();
				}
				if (statement != null) {
					statement.close();
				}
			}
			break;
		}
		return (dbtrace);
	}

	protected HashSet OrderStatusDB(Properties obj, Connection con)
			throws java.sql.SQLException {

		boolean resubmit = Boolean.parseBoolean((String) obj.get("resubmit"));
		HashSet dbtrace = new HashSet();

		while (true) {
			PreparedStatement statement = null;
			ResultSet rs = null;
			String cursor = null;

			try {
				Date NetStartTime = new java.util.Date();

				statement = con
						.prepareStatement("select tpcc_orderstatus(?,?,?,?)");

				statement.setInt(1, Integer.parseInt((String) obj.get("wid")));
				statement.setInt(2, Integer.parseInt((String) obj.get("did")));
				statement.setInt(3, Integer.parseInt((String) obj.get("cid")));
				statement.setString(4, (String) obj.get("lastname"));
				rs = statement.executeQuery();

				if (rs.next()) {
					cursor = (String) rs.getString(1);
				}
				rs.close();
				rs = null;
				statement.close();
				statement = null;
				statement = con.prepareStatement("fetch all in \"" + cursor
						+ "\"");
				rs = statement.executeQuery();

				while (rs.next()) {
					dbtrace.add(rs.getString(1));
				}
				rs.close();
				rs = null;
				statement.close();
				statement = null;

				Date NetFinishTime = new java.util.Date();

				String str = (String) (obj).get("cid");
				if (str.equals("0")) {
					processLog(NetStartTime, NetFinishTime, "processing", "r",
							"tx orderstatus 01");
				} else {
					processLog(NetStartTime, NetFinishTime, "processing", "r",
							"tx orderstatus 02");
				}

			} catch (java.sql.SQLException sqlex) {
				logger
						.warn("OrderStatus - SQL Exception "
								+ sqlex.getMessage());
				String str = (String) (obj).get("cid");
				if ((sqlex.getMessage().indexOf("serialize") != -1)
						|| (sqlex.getMessage().indexOf("deadlock") != -1)) {
					if (str.equals("0")) {
						RollbackTransaction(con, sqlex, "tx orderstatus 01",
								"r");
					} else {
						RollbackTransaction(con, sqlex, "tx orderstatus 02",
								"r");
					}

					if (resubmit) {
						if (str.equals("0")) {
							InitTransaction(con, "tx orderstatus 01", "r");
						} else {
							InitTransaction(con, "tx orderstatus 02", "r");
						}
						continue;
					} else {
						throw sqlex;
					}
				} else {
					if (str.equals("0")) {
						RollbackTransaction(con, sqlex, "tx orderstatus 01",
								"r");
					} else {
						RollbackTransaction(con, sqlex, "tx orderstatus 02",
								"r");
					}
					throw sqlex;
				}
			} catch (java.lang.Exception ex) {
				logger.fatal("Unexpected error. Something bad happend");
				ex.printStackTrace(System.err);
				System.exit(-1);
			} finally {
				if (rs != null) {
					rs.close();
				}
				if (statement != null) {
					statement.close();
				}
			}
			break;
		}
		return (dbtrace);
	}

	protected HashSet PaymentDB(Properties obj, Connection con)
			throws java.sql.SQLException {

		boolean resubmit = Boolean.parseBoolean((String) obj.get("resubmit"));
		HashSet dbtrace = new HashSet();
		StringBuffer select = new StringBuffer();
		while (true) {
			PreparedStatement statement = null;
			ResultSet rs = null;
			String cursor = null;

			try {
				Date NetStartTime = new java.util.Date();

				select.append("select * from handleSelect('");
				if (obj.get("lastname") != null && obj.get("cwid") != null
						&& obj.get("cdid") != null) {
					select.append("select * from customer where c_last = \\'"
							+ obj.get("lastname") + "\\' and c_w_id = "
							+ obj.get("cwid") + " and c_d_id = "
							+ obj.get("cdid") + ";");
				}
				if (obj.get("cid") != null && obj.get("cwid") != null
						&& obj.get("cid") != null)
					select.append("select * from customer where c_id = "
							+ obj.get("cid") + " and c_w_id = "
							+ obj.get("cwid") + " and c_d_id = "
							+ obj.get("cid") + ";");
				if (obj.get("wid") != null && obj.get("did") != null)
					select.append("select * from district where d_w_id = "
							+ obj.get("wid") + " and d_id = " + obj.get("did")
							+ ";");
				if (obj.get("wid") != null)
					select.append("select * from warehouse where w_id = "
							+ obj.get("wid") + ";");

				select.append("');");
				statement = con
						.prepareStatement("select tpcc_payment(?,?,cast(? as numeric(6,2)),?,?,?,cast(? as char(16)))");

				statement.setInt(1, Integer.parseInt((String) obj.get("wid")));
				statement.setInt(2, Integer.parseInt((String) obj.get("cwid")));
				statement.setFloat(3, Float.parseFloat((String) obj
						.get("hamount")));
				statement.setInt(4, Integer.parseInt((String) obj.get("did")));
				statement.setInt(5, Integer.parseInt((String) obj.get("cdid")));
				statement.setInt(6, Integer.parseInt((String) obj.get("cid")));
				statement.setString(7, (String) obj.get("lastname"));

				rs = statement.executeQuery();

				if (rs.next()) {
					cursor = (String) rs.getString(1);
				}
				rs.close();
				rs = null;
				statement.close();
				statement = null;
				statement = con.prepareStatement("fetch all in \"" + cursor
						+ "\"");
				rs = statement.executeQuery();

				while (rs.next()) {
					dbtrace.add(rs.getString(1));
				}
				rs.close();
				rs = null;
				statement.close();
				statement = null;
				statement = con.prepareStatement(select.toString());
				statement.executeQuery();
				Date NetFinishTime = new java.util.Date();

				String str = (String) (obj).get("cid");
				if (str.equals("0")) {
					processLog(NetStartTime, NetFinishTime, "processing", "w",
							"tx payment 01");
				} else {
					processLog(NetStartTime, NetFinishTime, "processing", "w",
							"tx payment 02");
				}
			} catch (java.sql.SQLException sqlex) {
				logger.warn("Payment - SQL Exception " + sqlex.getMessage());
				String str = (String) (obj).get("cid");
				if ((sqlex.getMessage().indexOf("serialize") != -1)
						|| (sqlex.getMessage().indexOf("deadlock") != -1)) {

					if (str.equals("0")) {
						RollbackTransaction(con, sqlex, "tx payment 01", "w");
					} else {
						RollbackTransaction(con, sqlex, "tx payment 02", "w");
					}

					if (resubmit) {
						if (str.equals("0")) {
							InitTransaction(con, "tx payment 01", "w");
						} else {
							InitTransaction(con, "tx payment 02", "w");
						}
						continue;
					} else {
						throw sqlex;
					}
				} else {
					if (str.equals("0")) {
						RollbackTransaction(con, sqlex, "tx payment 01", "w");
					} else {
						RollbackTransaction(con, sqlex, "tx payment 02", "w");
					}
					throw sqlex;
				}
			} catch (java.lang.Exception ex) {
				logger.fatal("Unexpected error. Something bad happend");
				ex.printStackTrace(System.err);
				System.exit(-1);
			} finally {
				if (rs != null) {
					rs.close();
				}
				if (statement != null) {
					statement.close();
				}
			}
			break;
		}
		return (dbtrace);
	}

	protected HashSet StockLevelDB(java.util.Properties obj, Connection con)
			throws java.sql.SQLException {

		boolean resubmit = Boolean.parseBoolean((String) obj.get("resubmit"));
		HashSet dbtrace = new HashSet();

		while (true) {
			PreparedStatement statement = null;
			ResultSet rs = null;
			String cursor = null;

			try {
				Date NetStartTime = new java.util.Date();

				statement = con
						.prepareStatement("select tpcc_stocklevel(?,?,?)");

				statement.setInt(1, Integer.parseInt((String) obj.get("wid")));
				statement.setInt(2, Integer.parseInt((String) obj.get("did")));
				statement.setInt(3, Integer.parseInt((String) obj
						.get("threshhold")));
				rs = statement.executeQuery();

				if (rs.next()) {
					cursor = (String) rs.getString(1);
				}
				rs.close();
				rs = null;
				statement.close();
				statement = null;
				statement = con.prepareStatement("fetch all in \"" + cursor
						+ "\"");
				rs = statement.executeQuery();

				while (rs.next()) {
					dbtrace.add(rs.getString(1));
				}
				rs.close();
				rs = null;
				statement.close();
				statement = null;

				Date NetFinishTime = new java.util.Date();

				processLog(NetStartTime, NetFinishTime, "processing", "r",
						"tx stocklevel");

			} catch (java.sql.SQLException sqlex) {
				logger.warn("StockLevel - SQL Exception " + sqlex.getMessage());
				if ((sqlex.getMessage().indexOf("serialize") != -1)
						|| (sqlex.getMessage().indexOf("deadlock") != -1)) {
					RollbackTransaction(con, sqlex, "tx stocklevel", "r");
					if (resubmit) {
						InitTransaction(con, "tx stocklevel", "r");
						continue;
					} else {
						throw sqlex;
					}
				} else {
					RollbackTransaction(con, sqlex, "tx stocklevel", "r");
					throw sqlex;
				}
			} catch (java.lang.Exception ex) {
				logger.fatal("Unexpected error. Something bad happend");
				ex.printStackTrace(System.err);
				System.exit(-1);
			} finally {
				if (rs != null) {
					rs.close();
				}
				if (statement != null) {
					statement.close();
				}
			}
			break;
		}
		return (dbtrace);
	}

	protected void InitTransaction(Connection con, String strTrans,
			String strAccess) throws java.sql.SQLException {
		Statement statement = null;
		try {
			Date NetStartTime = new java.util.Date();

			statement = con.createStatement();
			statement.execute("begin transaction");
			statement.execute("set transaction isolation level serializable");
			statement.execute("select '" + strTrans + "'");

			Date NetFinishTime = new java.util.Date();

			processLog(NetStartTime, NetFinishTime, "beginning", strAccess,
					strTrans);

		} catch (java.lang.Exception ex) {
			logger.fatal("Unexpected error. Something bad happend");
			ex.printStackTrace(System.err);
			System.exit(-1);
		} finally {
			if (statement != null) {
				statement.close();
			}
		}
	}

	protected void CommitTransaction(Connection con, String strTrans,
			String strAccess) throws java.sql.SQLException {
		{
			Statement statement = null;
			try {
				Date NetStartTime = new java.util.Date();

				statement = con.createStatement();
				statement.execute("commit transaction");

				Date NetFinishTime = new java.util.Date();

				processLog(NetStartTime, NetFinishTime, "committing",
						strAccess, strTrans);

			} catch (java.sql.SQLException sqlex) {
				RollbackTransaction(con, sqlex, strTrans, strAccess);
				throw sqlex;
			} catch (java.lang.Exception ex) {
				logger.fatal("Unexpected error. Something bad happend");
				ex.printStackTrace(System.err);
				System.exit(-1);
			} finally {
				if (statement != null) {
					statement.close();
				}
			}
		}
	}

	protected void RollbackTransaction(Connection con,
			java.lang.Exception dump, String strTrans, String strAccess)
			throws java.sql.SQLException {
		Statement statement = null;
		try {
			Date NetStartTime = new java.util.Date();

			statement = con.createStatement();
			statement.execute("rollback transaction");

			Date NetFinishTime = new java.util.Date();

			processLog(NetStartTime, NetFinishTime, "aborting", strAccess,
					strTrans);
		} catch (java.lang.Exception ex) {
			logger.warn("Error rolling back");
		} finally {
			if (statement != null) {
				statement.close();
			}
		}
	}
}
// arch-tag: 5e93fc99-eedb-49eb-af2a-bbdb57146184

