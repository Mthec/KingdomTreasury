package mod.wurmunlimited.treasury;

public class Transaction {
    public final byte kingdom;
    public final TransactionType type;

    public Transaction(byte kingdom, TransactionType type) {
        this.kingdom = kingdom;
        this.type = type;
    }
}
