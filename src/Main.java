import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.io.*;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.text.*;

/**
 * Classe principale che legge del testo e delle immagini da file e li stampa su carta.
 */
public class Main implements Printable {

    // Variabile globale per memorizzare il testo stilizzato da stampare.
    static AttributedString myStyledText = null;

    // Variabile globale per memorizzare le immagini da stampare.
    static ArrayList<BufferedImage> images = new ArrayList<>();

    // Variabile globale per memorizzare il tipo di file selezionato
    static String fileType = "";

    /**
     * Metodo principale dell'applicazione.
     * @param args Argomenti della riga di comando (non utilizzati).
     */
    public static void main(String[] args) {
        // Mostra la finestra di dialogo per la selezione del file
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        // Aggiunge filtri per i file di testo e immagini
        FileNameExtensionFilter textFilter = new FileNameExtensionFilter("Text files", "txt", "log", "json", "py", "java", "bat", "");
        FileNameExtensionFilter imageFilter = new FileNameExtensionFilter("Images", "jpg", "jpeg", "png", "gif");
        fileChooser.addChoosableFileFilter(textFilter);
        fileChooser.addChoosableFileFilter(imageFilter);
        fileChooser.setAcceptAllFileFilterUsed(false);

        int returnValue = fileChooser.showOpenDialog(null);

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String fileName = selectedFile.getAbsolutePath();
            fileType = getFileExtension(selectedFile);

            // Processa il file in base al suo tipo
            if (fileType.equalsIgnoreCase("txt") || fileType.equalsIgnoreCase("log") || fileType.equalsIgnoreCase("json") || fileType.equalsIgnoreCase("py") || fileType.equalsIgnoreCase("java") || fileType.equalsIgnoreCase("bat")) {
                // Legge il contenuto del file di testo e lo memorizza in una stringa
                String mText = readContentFromFileToPrint(fileName);
                // Crea un AttributedString dal testo letto
                myStyledText = new AttributedString(mText);
            } else if (fileType.equalsIgnoreCase("jpg") || fileType.equalsIgnoreCase("jpeg") || fileType.equalsIgnoreCase("png") || fileType.equalsIgnoreCase("gif")) {
                // Carica l'immagine da stampare
                loadImages(fileName);
            } else {
                System.err.println("File not supported.");
                return;
            }

            // Avvia il processo di stampa
            printToPrinter();
        } else {
            System.out.println("Operation cancelled.");
        }
    }

    /**
     * Legge il contenuto di un file di testo e lo restituisce come stringa.
     * @param fileName Il percorso del file da leggere.
     * @return Il contenuto del file come stringa.
     */
    private static String readContentFromFileToPrint(String fileName) {
        StringBuilder dataToPrint = new StringBuilder();

        try (BufferedReader input = new BufferedReader(new FileReader(fileName))) {
            String line;
            // Legge il file riga per riga e accumula il testo
            while ((line = input.readLine()) != null) {
                dataToPrint.append(line).append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace(); // Stampa la traccia dello stack per il debug in caso di errore
        }
        return dataToPrint.toString();
    }

    /**
     * Carica le immagini da stampare.
     * @param imageFileName Il percorso del file dell'immagine da caricare.
     */
    private static void loadImages(String imageFileName) {
        try {
            BufferedImage image = ImageIO.read(new File(imageFileName));
            if (image != null) {
                images.add(image);
                System.out.println("Loaded image: " + imageFileName + " with dimension " + image.getWidth() + "x" + image.getHeight());
            } else {
                System.err.println("Impossible to load the image: " + imageFileName);
            }
        } catch (IOException e) {
            System.err.println("Error during the image loading: " + e.getMessage());
            e.printStackTrace(); // Stampa la traccia dello stack per il debug in caso di errore
        }
    }

    /**
     * Configura il lavoro di stampa e avvia la finestra di dialogo per la stampa.
     */
    public static void printToPrinter() {
        // Ottiene un oggetto PrinterJob per gestire il lavoro di stampa
        PrinterJob printerJob = PrinterJob.getPrinterJob();

        // Crea un oggetto Book, che contiene uno o più oggetti Printable
        Book book = new Book();
        // Aggiunge l'oggetto Main (che implementa Printable) e la formattazione della pagina al Book
        book.append(new Main(), new PageFormat());
        // Imposta il Book come oggetto da stampare nel PrinterJob
        printerJob.setPageable(book);

        // Mostra la finestra di dialogo di stampa
        boolean doPrint = printerJob.printDialog();

        if (doPrint) {
            try {
                // Avvia il processo di stampa
                printerJob.print();
            } catch (PrinterException ex) {
                System.err.println("Error found while printing: " + ex);
            }
        }
    }

    /**
     * Metodo dell'interfaccia Printable che gestisce la stampa del contenuto.
     * @param g L'oggetto Graphics per disegnare il contenuto.
     * @param format La formattazione della pagina.
     * @param pageIndex L'indice della pagina da stampare.
     * @return Stato della stampa (PAGE_EXISTS se la pagina è stampata).
     */
    public int print(Graphics g, PageFormat format, int pageIndex) {
        Graphics2D graphics2d = (Graphics2D) g;
        // Trasla l'origine del grafico all'area immagine della pagina
        graphics2d.translate(format.getImageableX(), format.getImageableY());

        float y = 0; // Coordinata Y per la stampa del testo e delle immagini

        if (myStyledText != null) {
            // Stampa il testo
            Point2D.Float pen = new Point2D.Float();
            AttributedCharacterIterator charIterator = myStyledText.getIterator();
            LineBreakMeasurer measurer = new LineBreakMeasurer(charIterator, graphics2d.getFontRenderContext());
            float wrappingWidth = (float) format.getImageableWidth();

            while (measurer.getPosition() < charIterator.getEndIndex()) {
                TextLayout layout = measurer.nextLayout(wrappingWidth);
                pen.y += layout.getAscent();
                float dx = layout.isLeftToRight() ? 0 : (wrappingWidth - layout.getAdvance());
                layout.draw(graphics2d, pen.x + dx, pen.y);
                pen.y += layout.getDescent() + layout.getLeading();
            }
        }

        if (images.size() > 0) {
            // Stampa le immagini
            for (BufferedImage image : images) {
                if (image == null) {
                    System.err.println("null image found.");
                    continue;
                }
                if (y + image.getHeight() > format.getImageableHeight()) {
                    System.err.println("the image is too big for this page.");
                    return Printable.PAGE_EXISTS;
                }
                System.out.println("printing the image with the dimension " + image.getWidth() + "x" + image.getHeight() + " at the position Y=" + y);
                graphics2d.drawImage(image, 0, (int) y, null);
                y += image.getHeight(); // Sposta la posizione Y per la prossima immagine
            }
        }

        return Printable.PAGE_EXISTS;
    }

    /**
     * Ottiene l'estensione del file dato il file.
     * @param file Il file di cui ottenere l'estensione.
     * @return L'estensione del file.
     */
    private static String getFileExtension(File file) {
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1);
        } else {
            return "";
        }
    }
}
