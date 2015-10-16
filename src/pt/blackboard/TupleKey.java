package pt.blackboard;

public enum TupleKey {
	INPUT(1000),
	OUTPUT(1001),
	QUERY(1002),
	QUERYRESULT(1003),
	SEARCHROOM(1004),
	SEARCHCAR(1005),
	SEARCHTOUR(1006),
	SEARCHFORECAST(1007),
	SEARCHRATING(1008);
	
	private final int value;

    TupleKey(int value) {
        this.value = value;
    }
	
}
