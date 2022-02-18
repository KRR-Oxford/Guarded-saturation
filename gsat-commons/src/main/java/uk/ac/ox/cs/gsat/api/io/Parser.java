package uk.ac.ox.cs.gsat.api.io;

/**
 * Interface of parser of file or directory that contains some {@link uk.ac.ox.cs.gsat.fol.TGD}, {@link uk.ac.ox.cs.pdq.fol.Atom} and queries.
 */
public interface Parser {

    /**
     * parse the file
     */
    public ParserResult parse(String path) throws Exception;

}
