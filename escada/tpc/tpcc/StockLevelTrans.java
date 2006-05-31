package escada.tpc.tpcc;

import escada.tpc.common.Emulation;
import escada.tpc.common.StateObject;
import escada.tpc.common.util.RandGen;
import escada.tpc.tpcc.database.transaction.dbTPCCDatabase;

/**
 * It implements the states according to the definition of the TPC-C. Basically,
 * it sets up import information used in the execution of the stock level
 * transaction. Additionally, it defines the trace flag, which is a boolean
 * value used to log traces or not and the trace file.
 */
public class StockLevelTrans extends StateObject {
	public void initProcess(Emulation em, String hid) {
		int wid = (em.getEmulationId() / 10) + 1; // TODO -- Change to
		// constant...
		int did = 0;
		int threshhold = 0;

		outInfo.putInfo("resubmit", Boolean.toString(Emulation
				.getStatusReSubmit()));
		outInfo.putInfo("trace", Emulation.getTraceInformation());
		outInfo.putInfo("abort", "0");
		outInfo.putInfo("hid", hid);

		outInfo.putInfo("wid", Integer.toString(wid));
		if (((em.getEmulationId() + 1) % 10) == 0) {
			outInfo.putInfo("did", Integer.toString(10));
		} else {
			outInfo.putInfo("did", Integer
					.toString((em.getEmulationId() + 1) % 10));
		}

		threshhold = RandGen.nextInt(em.getRandom(),
				TPCCConst.numINIThreshHold, TPCCConst.numENDThreshHold + 1);
		outInfo.putInfo("threshhold", Integer.toString(threshhold));
		outInfo.putInfo("thinktime", Long.toString(em.getThinkTime()));
		outInfo.putInfo("file", em.getEmulationName());
	}

	public void prepareProcess(Emulation em, String hid) {

	}

	public Object requestProcess(Emulation em, String hid) {
		Object requestProcess = null;
		dbTPCCDatabase db = (dbTPCCDatabase) em.getDatabase();
		try {
			initProcess(em, hid);
			requestProcess = db.TraceStockLevelDB(outInfo, hid);
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
		return (requestProcess);
	}

	public void postProcess(Emulation em, String hid) {
		inInfo.resetInfo();
		outInfo.resetInfo();
	}

	public void setProb() {
		prob = 4;
	}

	public void setKeyingTime() {
		keyingtime = 2;
	}

	public void setThinkTime() {
		thinktime = 5;
	}

	public String toString() {
		return ("StockLevelTrans");
	}
}// arch-tag: 0694849b-7a20-4b66-872a-668c3adf88f0
