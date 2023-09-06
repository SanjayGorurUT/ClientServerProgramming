public class LoanPair {
    private int loanId;
    private String book;

    public LoanPair(int loanId, String book) {
        this.loanId = loanId;
        this.book = book;
    }

    public int getLoanId() {
        return this.loanId;
    }

    public String getBook() {
        return this.book;
    }
}
