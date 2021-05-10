//dati di un utente nella specifica sfida
public class Session {
    private String word;
    private int numWord;
    private String username;
    public int points;

    public Session(String word, int numWord) {
        this.word = word;
        this.numWord = numWord;
        this.username = null;
        this.points = 0;
    }

    public void setWord(String neww) {
        word = neww;
    }

    public void setUsername (String name) {
        username = name;
    }

    public String getWord() {
        return word;
    }

    public int getIndex() {
        return numWord;
    }

    public String getUsername () {
        return username;
    }

    public int getPoints() {
        return points;
    }

    public void incIndex() {
        numWord ++;
    }
}