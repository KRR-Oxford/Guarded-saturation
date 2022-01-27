package uk.ac.ox.cs.gsat.fol;

import java.util.Objects;

import uk.ac.ox.cs.pdq.ClassManager;

public class Cache {

	protected static ClassManager<TGD> tgd = null;
    protected static ClassManager<GTGD> gtgd = null;
    protected static ClassManager<SkGTGD> skgtgd = null;
    protected static ClassManager<OrderedSkGTGD> ordskgtgd = null;

	static {
		startCaches();
	}

	public static void reStartCaches() {
		tgd.reset();
        gtgd.reset();
        skgtgd.reset();
        ordskgtgd.reset();
	}

	private static synchronized void startCaches() {
		tgd = createManager();
		gtgd = createManager();
        skgtgd = createManager();
        ordskgtgd = createManager();
	}

    private static <T extends TGD> ClassManager<T> createManager() {
        return new ClassManager<T>() {
			protected boolean equal(T object1, T object2) {
				return object1.getBodySet().equals(object2.getBodySet()) && object1.getHeadSet().equals(object2.getHeadSet());
			}

			protected int getHashCode(T object) {
                return Objects.hash(object.getBodySet(), object.getHeadSet());
			}
		};
    }
}
