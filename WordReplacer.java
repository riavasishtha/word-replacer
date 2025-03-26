import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/** Performs global search & replaces words in a file.
 * The program accepts 3  arguments: input text file (1), word replacement rules (2), data structure of choice (3).
 * @author Ria Vasishtha
 * @author UNI: rv2529
 */
public class WordReplacer {


    /**
     * This is the starting point for the program.
     * Validates user's inputted arguments & calls the respective methods to read/process the files.
     * @param args Command-line arguments format: <input text file> <word replacements file> <bst|rbt|hash>.
     */
    public static void main(String[] args) {
        // starting point for the program
        int numArgs = args.length; // checks if user inputted correct num of args
        if (numArgs != 3) {
            System.err.print("Usage: java WordReplacer <input text file> <word replacements file> <bst|rbt|hash>\n");
            System.exit(1);
        }

        // bc of ^^ lines we know that args[0,1,2] exist
        String inputTextFile = args[0];
        String wordReplacementFile = args[1];
        String dataStructure = args[2];

        // check all the args and validate them
        FileReader inputReader = null;
        try {
            inputReader = new FileReader(inputTextFile);
        } catch (FileNotFoundException e) {
            // had to use \n vs ln bc output wasn't consistent
            System.err.print("Error: Cannot open file '" + inputTextFile + "' for input.\n");
            System.exit(1);
        }

        // by creating reader outside of it we can reference it after
        FileReader replacementReader = null;
        try {
            replacementReader = new FileReader(wordReplacementFile);
        } catch (FileNotFoundException e) {
            System.err.print("Error: Cannot open file '" + wordReplacementFile + "' for input.\n");
            System.exit(1);
        }

        MyMap<String, String> map = null;
        if (!dataStructure.equals("bst") && !dataStructure.equals("rbt") && !dataStructure.equals("hash")) {
            System.err.print("Error: Invalid data structure '" + dataStructure + "' received.\n");
            System.exit(1);
        } else { // we know atp that they have one of the 3
            if (dataStructure.equals("bst")) {
                map = new BSTreeMap<>();
            } else if (dataStructure.equals("rbt")) {
                map = new RBTreeMap<>();
            } else {
                map = new MyHashMap<>();
            }
        }

        // the actual stuff begins here
        parseRules(replacementReader, wordReplacementFile, map); // fills the map up
        String output = processText(inputReader, inputTextFile, map); // references the map to make replacements

        // readers close @ end -- source: https://stackoverflow.com/questions/29535973/is-it-necessary-to-close-files-after-reading-only-in-any-programming-language
        System.out.printf("%s\n", output); // prints it
    }

    /**
     * Parses the replacement rules & populates the user's map of choice.
     * Identifies cycles by using Kruskal's union-find approach & quits the program if cycle is detected.
     * @param rules FileReader used to read the replacement rules file.
     * @param fileName Replacement rules file name.
     * @param map This is the map (bst/rbt/hash) to store the relationships.
     */
    public static void parseRules(FileReader rules, String fileName, MyMap<String, String> map) {
        try {
            // 1) populate the map
            BufferedReader reader = new BufferedReader(rules);
            String line; // source: https://stackoverflow.com/questions/16104616/using-bufferedreader-to-read-text-file

            while ((line = reader.readLine()) != null) { // as long as the line isn't null process it
                int arrowStart = line.indexOf("-");
                String word = line.substring(0, arrowStart - 1); // this accounts for the [space]-
                String replacement = line.substring(arrowStart + 3); // contains beginning index

                // uses compressed kruskal's union-find alg from class (source: 12/02 recorded lecture)
                if (map.get(word) == null) { // if it doesn't exist on map
                    map.put(word, word); // add to the map
                }
                if (map.get(replacement) == null) {
                    map.put(replacement, replacement); // add it!
                }

                if (word.equals(replacement)) { // if the 2 words are = then it's auto a cycle no finding needed
                    System.err.print("Error: Cycle detected when trying to add replacement rule: " +
                            word + " -> " + replacement + "\n");
                    System.exit(1);
                }
                // if it's not the code will just continue
                String root1 = find(word, map); // find the roots of the two words
                String root2 = find(replacement, map);

                // checks if the finds are the same
                if (root1.equals(root2)) {
                    System.err.print("Error: Cycle detected when trying to add replacement rule: " +
                            word + " -> " + replacement + "\n");
                    System.exit(1);
                }

                // they're not the same! so union the roots
                map.put(root1, root2); // make them in the same set ie connect root 1 to 2
            }

        } catch (IOException e) {
            System.err.print("Error: An I/O error occurred reading '" + fileName + "'.\n");
            System.exit(1);
        }
    }

    /**
     * Identifies the "root" of a word, i.e. what the word maps to.
     * Uses recursion to identify the word's root & compresses the path to avoid lengthy lookup.
     * @param word Word to find the root of.
     * @param map Map storing the relationships.
     * @return Root of the word.
     */
    public static String find(String word, MyMap<String,String> map) {
        if (map.get(word) == null) { // this shouldn't occur but needed to add so .equals worked
            return word;
        }
        // base case!
        if (map.get(word).equals(word)) { // basically when it's word,word meaning no further "root"
            return word;
        }
        String root = find(map.get(word), map); // uses recursion to find parent
        map.put(word, root); // the compression step to make it easier to find next time
        return root;
    }

    /**
     * Applies the replacement rules to an input text file, replacing words based on the relationships in the map.
     * Uses character/non-character sequences to build a word & searches map for replacement.
     * Builds the revised text (output) one line at a time.
     * @param text FileReader used to read the input text file.
     * @param fileName Name of the input text file.
     * @param map Populated map storing the word relationships.
     * @return output.toString converts the StringBuilder to a String object holding the processed text.
     */
    public static String processText(FileReader text, String fileName, MyMap<String, String> map) {
        BufferedReader reader = new BufferedReader(text);
        String line;
        StringBuilder output = new StringBuilder();
        try {
            // figure out where to put word
            while((line = reader.readLine()) != null) { // null is talking abt end of the file
                StringBuilder currentWord = new StringBuilder();
                StringBuilder newLine = new StringBuilder();

                for (int i = 0; i < line.length(); i++) {
                    char c = line.charAt(i);

                    if (Character.isLetter(c)) {
                        currentWord.append(c); // keep building up word
                    }
                    else {
                        if (!currentWord.isEmpty()) { // ex it's not a space
                            String word = currentWord.toString(); // make string builder an object
                            if (map.get(word) != null) { // this checks that the word is in map
                                String replacement = find(word, map); // handles any sort of transitivity
                                newLine.append(replacement); // add the replacement to it
                            }
                            else {
                                newLine.append(word);
                            }
                            currentWord.setLength(0); // bc we're starting for new word now
                        }
                        newLine.append(c);
                    }
                }
                // if it ends on a word & not punctuation
                if (!currentWord.isEmpty()) {
                    String lastWord = currentWord.toString();
                    String lastReplacement = find(lastWord, map);
                    if (lastReplacement != null) {
                        newLine.append(lastReplacement);
                    }
                    else {
                        newLine.append(lastWord);
                    }
                }

                output.append(newLine);
                output.append("\n"); // add a line after
            }
        }
        catch (IOException e) {
            System.err.print("Error: An I/O error occurred reading '" + fileName + "'.\n");
            System.exit(1);
        }
        return output.toString(); // convert from stringbuilder to a string to be printed later
    }
}
