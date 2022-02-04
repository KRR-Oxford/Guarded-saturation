package uk.ac.ox.cs.gsat.satalg;

public enum NewTGDStructure {
    STACK,
    /**
     * In evolved based algorithms, if true, the new TGDs (right and left) are
     * stored in ordered sets such that the TGDs with smallest body and largest head
     * come first
     */
    ORDERED_BY_ATOMS_NB, SET
}
