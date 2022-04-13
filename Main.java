package live.kazutree;

import com.github.jikyo.romaji.Transliterator;
import org.mozilla.universalchardet.UniversalDetector;

import javax.swing.*;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Main {
    static String pickFolder() {
        JFileChooser picker = new JFileChooser();
        picker.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        picker.setDialogTitle("Select the osu! directory");
        picker.setCurrentDirectory(new File("C:\\"));

        int result = picker.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION)
            return picker.getSelectedFile().getAbsolutePath();

        JOptionPane.showMessageDialog(null, "Failed to select osu! directory!");
        System.exit(0);
        return null;
    }

    static String purge(String in) {
        String translated = Transliterator.transliterate(in).get(0); // Translate because some people in this community name their skins in Japanese

        String removed = translated.replaceAll("[^a-zA-Z0-9\s.]", " "); // Remove all characters that aren't letters, numbers, "." or whitespace
        removed = removed.replaceAll("\\s{2,}", " ").trim(); // make 2 spaces into one, remove all whitespace at the start

        return removed;
    }

    static void fixIniFile(File f, String newName) {
        try {
            // Really, shout-out to everyone who decided to use different encoding on their ini files.
            Charset encoding = Charset.forName(UniversalDetector.detectCharset(f));
            Path path = Paths.get(f.getAbsolutePath());
            List<String> fileContent = new ArrayList<>(Files.readAllLines(path, encoding));

            for (String s : fileContent) {
                if (s.contains("Name:")) { // This is case-sensitive, toLowerCase messes with unicode stuff anyway.
                    String properPadded = s.substring(0, s.indexOf(":") + 1); // We still care what the ini file looks like
                    String newLine = properPadded + " " + newName; // Add new name to the file with a space, formatting sake.

                    System.out.printf("[ini] (%s) => (%s)\n", s.trim(), newLine.trim());
                    fileContent.set(fileContent.indexOf(s), newLine);
                }
            }

            Files.write(path, fileContent, encoding);
        }
        catch (final Exception ex) {
            // Probably encoding couldn't be detected by the UniversalDetector. Thanks guys.
            //ex.printStackTrace();
        }
    }

    static void loop(String path) {
        File parent = new File(path);

        try {
            for (File skinFolder : parent.listFiles()) {
                if (skinFolder.isDirectory()) { // Should be a skin
                    String newName = purge(skinFolder.getName());

                    for (File f : skinFolder.listFiles()) {
                        if (f.getAbsolutePath().endsWith(".ini"))
                            fixIniFile(f, newName);
                    }

                    System.out.printf("[path] (%s) => (%s)\n\n", skinFolder.getName(), newName);
                    skinFolder.renameTo(new File(skinFolder.getParent() + "\\" + newName));
                }
            }
        }
        catch (final Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        JOptionPane.showMessageDialog(null, "Please ensure osu! is closed, and backup your skins!\nThese changes are irreversible!");

        String path = args.length == 0 ? pickFolder() : args[0];
        loop(path + "\\Skins");
    }
}
