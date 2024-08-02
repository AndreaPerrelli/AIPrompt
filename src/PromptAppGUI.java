import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Objects;
import javax.swing.UIManager.*;

public class PromptAppGUI extends JFrame {

    private JTextArea instructionArea;
    private JTextArea promptArea;
    private DefaultListModel<File> fileListModel;
    private final List<CodeContext> codeContexts = new ArrayList<>();
    private JComboBox<String> taskTypeComboBox;
    private WatchService watchService;

    public PromptAppGUI() {
        initializeLookAndFeel();
        setupMainFrame();
        setupTopPanel();
        setupCenterPanel();
        setupBottomPanel();
    }

    /**
     * Initializes the look and feel of the GUI.
     */
    private void initializeLookAndFeel() {
        try {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // Default look and feel will be used if Nimbus is not available.
        }
    }

    /**
     * Sets up the main frame of the application.
     */
    private void setupMainFrame() {
        setTitle("AI Prompt Application");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().add(new JPanel(new BorderLayout()));
    }

    /**
     * Sets up the top panel containing task type and instruction input.
     */
    private void setupTopPanel() {
        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = createGridBagConstraints();

        addTaskTypeComponents(topPanel, gbc);
        addTaskInstructionComponents(topPanel, gbc);

        getContentPane().add(topPanel, BorderLayout.NORTH);
    }

    /**
     * Creates and configures GridBagConstraints for layout management.
     * @return Configured GridBagConstraints.
     */
    private GridBagConstraints createGridBagConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        return gbc;
    }

    /**
     * Adds task type components to the top panel.
     * @param topPanel Panel to add components to.
     * @param gbc GridBagConstraints for layout management.
     */
    private void addTaskTypeComponents(JPanel topPanel, GridBagConstraints gbc) {
        JLabel taskTypeLabel = createLabel("Task Type:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        topPanel.add(taskTypeLabel, gbc);

        taskTypeComboBox = createComboBox(new String[]{"Feature", "Fix", "Refactor", "Question", "Blog", "Others"});
        gbc.gridx = 1;
        gbc.gridy = 0;
        topPanel.add(taskTypeComboBox, gbc);
    }

    /**
     * Adds task instruction components to the top panel.
     * @param topPanel Panel to add components to.
     * @param gbc GridBagConstraints for layout management.
     */
    private void addTaskInstructionComponents(JPanel topPanel, GridBagConstraints gbc) {
        JLabel taskInstructionLabel = createLabel("Task Instruction (Enter raw prompt here):");
        gbc.gridx = 0;
        gbc.gridy = 1;
        topPanel.add(taskInstructionLabel, gbc);

        instructionArea = createTextArea(5, 20);
        instructionArea.addFocusListener(createFocusListener(instructionArea));
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        topPanel.add(new JScrollPane(instructionArea), gbc);
    }

    /**
     * Creates a JLabel with specified text.
     * @param text Text to display on the label.
     * @return Configured JLabel.
     */
    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.PLAIN, 14));
        return label;
    }

    /**
     * Creates a JComboBox with specified items.
     * @param items Items to be displayed in the combo box.
     * @return Configured JComboBox.
     */
    private JComboBox<String> createComboBox(String[] items) {
        JComboBox<String> comboBox = new JComboBox<>(items);
        comboBox.setFont(new Font("Arial", Font.PLAIN, 14));
        return comboBox;
    }

    /**
     * Creates a JTextArea with specified rows and columns.
     * @param rows Number of rows.
     * @param columns Number of columns.
     * @return Configured JTextArea.
     */
    private JTextArea createTextArea(int rows, int columns) {
        JTextArea textArea = new JTextArea(rows, columns);
        textArea.setFont(new Font("Arial", Font.PLAIN, 14));
        textArea.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        return textArea;
    }

    /**
     * Creates a FocusAdapter for visual feedback on focus.
     * @param area JTextArea to apply focus listener to.
     * @return Configured FocusAdapter.
     */
    private java.awt.event.FocusAdapter createFocusListener(JTextArea area) {
        return new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                area.setBorder(BorderFactory.createLineBorder(Color.BLUE));
            }

            public void focusLost(java.awt.event.FocusEvent evt) {
                area.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            }
        };
    }

    /**
     * Sets up the center panel for file input.
     */
    private void setupCenterPanel() {
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createTitledBorder("File Input"));

        JLabel dragAndDropLabel = createLabel("Drag and drop files or folders here (VS Code/IDE/Finder/File Explorer).");
        centerPanel.add(dragAndDropLabel, BorderLayout.NORTH);

        fileListModel = new DefaultListModel<>();
        JList<File> fileList = new JList<>(fileListModel);
        fileList.setFont(new Font("Arial", Font.PLAIN, 14));
        fileList.setDropTarget(createDropTarget(dragAndDropLabel));
        fileList.addMouseListener(createFileListMouseListener(fileList));

        centerPanel.add(new JScrollPane(fileList), BorderLayout.CENTER);
        getContentPane().add(centerPanel, BorderLayout.CENTER);
    }

    /**
     * Creates a DropTarget for drag-and-drop functionality.
     * @param label JLabel for visual feedback.
     * @return Configured DropTarget.
     */
    private DropTarget createDropTarget(JLabel label) {
        return new DropTarget() {
            public synchronized void dragEnter(DropTargetDragEvent evt) {
                label.setForeground(Color.BLUE);
            }

            public synchronized void dragExit(DropTargetEvent evt) {
                label.setForeground(Color.BLACK);
            }

            public synchronized void drop(DropTargetDropEvent evt) {
                label.setForeground(Color.BLACK);
                handleFileDrop(evt);
            }
        };
    }

    /**
     * Handles the file drop event.
     * @param evt DropTargetDropEvent to handle.
     */
    private void handleFileDrop(DropTargetDropEvent evt) {
        try {
            evt.acceptDrop(DnDConstants.ACTION_COPY);
            List<File> droppedFiles = (List<File>)
                    evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
            for (File file : droppedFiles) {
                if (file.isDirectory()) {
                    addAllFilesFromDirectory(file);
                } else {
                    addFileToList(file);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Creates a MouseAdapter for file list interaction.
     * @param fileList JList for file display.
     * @return Configured MouseAdapter.
     */
    private java.awt.event.MouseAdapter createFileListMouseListener(JList<File> fileList) {
        return new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    int index = fileList.locationToIndex(evt.getPoint());
                    if (index >= 0) {
                        fileListModel.remove(index);
                        codeContexts.remove(index);
                        generatePrompt((String) Objects.requireNonNull(taskTypeComboBox.getSelectedItem()));
                    }
                }
            }
        };
    }

    /**
     * Sets up the bottom panel for final prompt display and actions.
     */
    private void setupBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createTitledBorder("Final Prompt"));

        bottomPanel.add(new JLabel("Final Prompt:"), BorderLayout.NORTH);
        promptArea = createTextArea(10, 20);
        bottomPanel.add(new JScrollPane(promptArea), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(createGenerateButton());
        buttonPanel.add(createCopyButton());

        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        getContentPane().add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * Creates the generate button with associated action.
     * @return Configured JButton.
     */
    private JButton createGenerateButton() {
        JButton generateButton = createButton("Generate Prompt");
        generateButton.addActionListener(_ -> generatePrompt((String) Objects.requireNonNull(taskTypeComboBox.getSelectedItem())));
        return generateButton;
    }

    /**
     * Creates the copy button with associated action.
     * @return Configured JButton.
     */
    private JButton createCopyButton() {
        JButton copyButton = createButton("Copy");
        copyButton.addActionListener(_ -> copyToClipboard(promptArea.getText()));
        return copyButton;
    }

    /**
     * Creates a JButton with specified text.
     * @param text Text to display on the button.
     * @return Configured JButton.
     */
    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.PLAIN, 14));
        button.addMouseListener(createButtonMouseListener(button));
        return button;
    }

    /**
     * Creates a MouseAdapter for button interaction.
     * @param button JButton for interaction.
     * @return Configured MouseAdapter.
     */
    private java.awt.event.MouseAdapter createButtonMouseListener(JButton button) {
        return new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(Color.LIGHT_GRAY);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(UIManager.getColor("control"));
            }
        };
    }

    /**
     * Adds all files from a directory to the file list.
     * @param directory Directory to add files from.
     */
    private void addAllFilesFromDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    addAllFilesFromDirectory(file);
                } else {
                    addFileToList(file);
                }
            }
        }
    }

    /**
     * Adds a file to the file list and code contexts.
     * @param file File to add.
     */
    private void addFileToList(File file) {
        fileListModel.addElement(file);
        try {
            String code = new String(Files.readAllBytes(file.toPath()));
            codeContexts.add(new CodeContext(file.getName(), code));
            watchFile(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Watches a file for changes.
     * @param file File to watch.
     * @throws IOException if an I/O error occurs.
     */
    private void watchFile(File file) throws IOException {
        if (watchService == null) {
            watchService = FileSystems.getDefault().newWatchService();
            new Thread(this::watchForChanges).start();
        }
        file.toPath().getParent().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
    }

    /**
     * Watches for changes in the files and updates the prompt.
     */
    private void watchForChanges() {
        try {
            WatchKey key;
            while ((key = watchService.take()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                        Path changed = (Path) event.context();
                        for (CodeContext context : codeContexts) {
                            if (context.getFileName().equals(changed.getFileName().toString())) {
                                String code = new String(Files.readAllBytes(changed));
                                context.setCode(code);
                                generatePrompt((String) Objects.requireNonNull(taskTypeComboBox.getSelectedItem()));
                            }
                        }
                    }
                }
                key.reset();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Generates the prompt based on the selected task type and code contexts.
     * @param taskType Type of task selected.
     */
    private void generatePrompt(String taskType) {
        StringBuilder prompt = new StringBuilder();
        switch (taskType) {
            case "Fix":
                prompt.append("You are tasked to fix a bug. Instructions are as follows:\n\n");
                break;
            case "Refactor":
                prompt.append("You are tasked to do a code refactoring. Instructions are as follows:\n\n");
                break;
            case "Question":
                prompt.append("You are tasked to answer a question:\n\n");
                break;
            case "Blog":
                prompt.append("You are tasked to write a blog post. Instructions are as follows:\n\n");
                break;
            case "Others":
                prompt.append("\n\n");
                break;
            default:
                prompt.append("You are tasked to implement a feature. Instructions are as follows:\n\n");
                break;
        }
        prompt.append(instructionArea.getText()).append("\n\n");
        prompt.append("Instructions for the output format:\n");
        prompt.append("- Output code without descriptions, unless it is important.\n");
        prompt.append("- Minimize prose, comments and empty lines.\n");
        prompt.append("- Only show the relevant code that needs to be modified. Use comments to represent the parts that are not modified.\n");
        prompt.append("- Make it easy to copy and paste.\n");
        prompt.append("- Consider other possibilities to achieve the result, do not be limited by the prompt.\n\n");
        prompt.append("Code Context:\n");
        for (CodeContext codeContext : codeContexts) {
            prompt.append("File: ").append(codeContext.getFileName()).append("\n");
            prompt.append("```\n").append(codeContext.getCode()).append("```\n\n");
        }
        promptArea.setText(prompt.toString());
    }

    /**
     * Copies the specified text to the system clipboard.
     * @param text Text to copy.
     */
    private void copyToClipboard(String text) {
        StringSelection stringSelection = new StringSelection(text);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }

    /**
     * Main method to launch the application.
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PromptAppGUI app = new PromptAppGUI();
            app.setVisible(true);
        });
    }
}

class CodeContext {
    private final String fileName;
    private String code;

    public CodeContext(String fileName, String code) {
        this.fileName = fileName;
        this.code = code;
    }

    public String getFileName() {
        return fileName;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
