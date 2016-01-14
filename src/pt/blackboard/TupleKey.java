package pt.blackboard;

public enum TupleKey {
	PROXYIN(10001),
	PROXYOUT(10002),
	PARSEIN(10003),
	PARSEOUT(1004),
	CONCEPTSIN(1005),
	CONCEPTSOUT(1006),
	TERMSIN(1007),
	TERMSOUT(1008),
	CALCULUSIN(1009),
	CALCULUSOUT(1010),
	OWLIN(1011),
	OWLOUT(1012),
	LOGIN(1013);
	
	private final int value;

    TupleKey(int value) {
        this.value = value;
    }
	
}
