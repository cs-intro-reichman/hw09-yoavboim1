import java.util.HashMap;
import java.util.Random;

public class LanguageModel {

    // The map of this model.
    // Maps windows to lists of charachter data objects.
    HashMap<String, List> dataMap;
    
    // The window length used in this model.
    int wLen;
    
    // The random number generator used by this model. 
    private Random rnd;

    /** Constructs a language model with the given window length and a given
      * seed value. Generating texts from this model multiple times with the 
      * same seed value will produce the same random texts. Good for debugging. */
    public LanguageModel(int windowLength, int seed) {
        this.wLen = windowLength;
        rnd = new Random(seed);
        dataMap = new HashMap<String, List>();
    }

    /** Constructs a language model with the given window length.
      * Generating texts from this model multiple times will produce
      * different random texts. Good for production. */
    public LanguageModel(int windowLength) {
        this.wLen = windowLength;
        rnd = new Random();
        dataMap = new HashMap<String, List>();
    }

    /** Builds a language model from the text in the given file (the corpus). */
    public void train(String fileName) {
        In reader = new In(fileName);
        StringBuilder buffer = new StringBuilder();
        while (buffer.length() < wLen && reader.hasNextChar()) {
            buffer.append(reader.readChar());
        }
        if (buffer.length() < wLen) return;
        String window = buffer.toString();

        while (reader.hasNextChar()) {
            char c = reader.readChar();
            List list = dataMap.get(window);
            if (list == null) {
                list = new List();
                dataMap.put(window, list);
            }
            list.update(c);
            window = window.substring(1) + c;
        }

        for (String key : dataMap.keySet()) {
            calculateProbabilities(dataMap.get(key));
        }
    }

    // Computes and sets the probabilities (p and cp fields) of all the
    // characters in the given list. */
    void calculateProbabilities(List list) {                
        if (list == null || list.getSize() == 0) return;

        CharData[] arr = list.toArray();
        int total = 0;
        for (int i = 0; i < arr.length; i++) {
            total += arr[i].count;
        }

        double cumulative = 0.0;
        for (int i = 0; i < arr.length; i++) {
            CharData cd = arr[i];
            cd.p = ((double) cd.count) / total;
            cumulative += cd.p;
            cd.cp = cumulative;
        }
    }

    // Returns a random character from the given probabilities list.
    char getRandomChar(List list) {
        if (list == null || list.getSize() == 0) return ' ';
        double r = rnd.nextDouble();
        CharData[] arr = list.toArray();
        for (int i = 0; i < arr.length; i++) {
            if (r < arr[i].cp) {
                return arr[i].chr;
            }
        }
        return arr[arr.length - 1].chr;
    }

    /**
     * Generates a random text, based on the probabilities that were learned during training. 
     * @param initialText - text to start with. If initialText's last substring of size numberOfLetters
     * doesn't appear as a key in Map, we generate no text and return only the initial text. 
     * @param numberOfLetters - the size of text to generate
     * @return the generated text
     */
    public String generate(String initialText, int textLength) {
        if (initialText == null) initialText = "";
        if (initialText.length() < wLen) return initialText;

        StringBuilder gen = new StringBuilder(initialText);
        String window = gen.substring(gen.length() - wLen);
        int endLength = initialText.length() + textLength;

        while (gen.length() < endLength) {
            List list = dataMap.get(window);
            if (list == null || list.getSize() == 0) {
                break;
            }
            char next = getRandomChar(list);
            gen.append(next);
            window = gen.substring(gen.length() - wLen);
        }

        return gen.toString();
    }

    /** Returns a string representing the map of this language model. */
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (String key : dataMap.keySet()) {
            List keyProbs = dataMap.get(key);
            str.append(key + " : " + keyProbs + "\n");
        }
        return str.toString();
    }

    public static void main(String[] args) {
        int wLen = Integer.parseInt(args[0]);
        String initialText = args[1];
        int generatedTextLength = Integer.parseInt(args[2]);
        boolean randomGeneration = args[3].equals("random");
        String fileName = args[4];

        LanguageModel lm = randomGeneration ? new LanguageModel(wLen)
                : new LanguageModel(wLen, 20);
        lm.train(fileName);
        System.out.println(lm.generate(initialText, generatedTextLength));
    }
}