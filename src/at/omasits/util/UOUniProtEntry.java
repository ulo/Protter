package at.omasits.util;

import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.model.uniprot.UniProtEntryImpl;

public class UOUniProtEntry extends UniProtEntryImpl {
	public Integer offsetFrom;
	public Integer offsetTo;
	
	public UOUniProtEntry(UniProtEntry up, Integer offsetFrom, Integer offsetTo) {
		super(up);
		this.offsetFrom = offsetFrom;
		this.offsetTo = offsetTo==null ? up.getSequence().getLength() : offsetTo;
	}

	public String getSequenceString() throws Exception {
		if (offsetTo > super.getSequence().getLength() || offsetFrom > super.getSequence().getLength())
			throw new Exception("Protein sub-sequence cannot extend protein sequence, which is "+super.getSequence().getLength()+" amino acids.");
		if (offsetFrom < 1 || offsetTo < 1)
			throw new Exception("Protein sub-sequence cannot start at zero or negative index.");
		if (offsetFrom > offsetTo)
			throw new Exception("Invalid protein sub-sequence: "+offsetFrom+"-"+offsetTo);
		return super.getSequence().getValue().substring(offsetFrom-1, offsetTo);
	}
	
}
