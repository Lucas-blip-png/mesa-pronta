package dev.lucas.mesapronta.reminder;

public enum ReminderType {

	H24("24 horas"), H1("1 hora");

	private final String label;

	ReminderType(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

}
