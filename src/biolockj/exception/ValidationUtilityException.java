package biolockj.exception;

public class ValidationUtilityException extends BioLockJException {

	public ValidationUtilityException( final Exception ex, final String msg ) {
		super( msg );
		ex.printStackTrace();
	}

	private static final long serialVersionUID = 5727347342836154743L;

}
