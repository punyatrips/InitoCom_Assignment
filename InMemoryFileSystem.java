import java.io.*;
import java.util.*;

// Represents the main in-memory file system
class FileSystem implements Serializable {
    private Directory root;
    Directory currentDirectory;

    // Constructor initializes the root and current directory
    public FileSystem() {
        root = new Directory("/"); 
        currentDirectory = root;
    }

    // Creates a new directory
    public void mkdir(String directoryName) {
        Directory newDirectory = new Directory(directoryName);
        currentDirectory.addDirectory(newDirectory);
    }

    // Changes the current directory
    public void cd(String path) {
        Directory newDirectory;
        if (path.equals("/")) {
            newDirectory = root;
        } else if (path.equals("..")) {
            newDirectory = currentDirectory.getParent();
        } else if (path.startsWith("/")) {
            newDirectory = findDirectoryByPath(root, path);
        } else {
            newDirectory = findDirectoryByPath(currentDirectory, path);
        }

        if (newDirectory != null) {
            currentDirectory = newDirectory;
        } else {
            System.out.println("Invalid path");
        }
    }

    // Finds a directory by path
    private Directory findDirectoryByPath(Directory current, String path) {
        String[] pathComponents = path.split("/");
        Directory temp = current;

        for (String component : pathComponents) {
            if (!component.isEmpty()) {
                temp = temp.getSubDirectory(component);
                if (temp == null) {
                    return null;
                }
            }
        }
        return temp;
    }

    // Lists the contents of the current or specified directory
    public void ls(String path) {
        Directory targetDirectory;
        if (path == null || path.isEmpty()) {
            targetDirectory = currentDirectory;
        } else if (path.equals("/")) {
            targetDirectory = root;
        } else {
            targetDirectory = findDirectoryByPath(currentDirectory, path);
        }

        if (targetDirectory != null) {
            System.out.println(targetDirectory.listContents());
        } else {
            System.out.println("Invalid path");
        }
    }

    // Creates a new empty file
    public void touch(String fileName) {
        File newFile = new File(fileName);
        currentDirectory.addFile(newFile);
    }

    // Writes text to a file
    public void echo(String fileName, String content) {
        File file = currentDirectory.getFile(fileName);

        // If the file doesn't exist, create a new one
        if (file == null) {
            file = new File(fileName);
            currentDirectory.addFile(file);
        }

        // Append content to the existing content of the file
        file.setContent(file.getContent() + content);
    }

    // Displays the contents of a file
    public void cat(String fileName) {
        File file = currentDirectory.getFile(fileName);
        if (file != null) {
            System.out.println(file.getContent());
        } else {
            System.out.println("File not found");
        }
    }

    // Searches for a string in files within the current directory
    public void grep(String searchString) {
        grepInDirectory(currentDirectory, searchString);
    }

    // Copies a file or directory to another location
    public void cp(String sourcePath, String destinationPath) {
        File sourceFile = findFileByPath(currentDirectory, sourcePath);
        if (sourceFile != null) {
            File destinationFile = sourceFile.clone();
            Directory destinationDirectory = findDirectoryByPath(currentDirectory, destinationPath);

            if (destinationDirectory != null) {
                destinationDirectory.addFile(destinationFile);
            } else {
                System.out.println("Invalid destination path");
            }
        } else {
            System.out.println("File not found");
        }
    }

    // Finds a file by path
    private File findFileByPath(Directory current, String path) {
        String[] pathComponents = path.split("/");
        String fileName = pathComponents[pathComponents.length - 1];

        Directory targetDirectory = findDirectoryByPath(current, path.substring(0, path.length() - fileName.length()));
        return (targetDirectory != null) ? targetDirectory.getFile(fileName) : null;
    }

    // Moves a file or directory to another location
    public void mv(String sourcePath, String destinationPath) {
        File sourceFile = findFileByPath(currentDirectory, sourcePath);
        if (sourceFile != null) {
            Directory destinationDirectory = findDirectoryByPath(currentDirectory, destinationPath);

            if (destinationDirectory != null) {
                destinationDirectory.addFile(sourceFile);
                currentDirectory.removeFile(sourceFile);
            } else {
                System.out.println("Invalid destination path");
            }
        } else {
            System.out.println("File not found");
        }
    }

    // Removes a file or directory
    public void rm(String path) {
        File fileToRemove = findFileByPath(currentDirectory, path);
        if (fileToRemove != null) {
            currentDirectory.removeFile(fileToRemove);
        } else {
            Directory directoryToRemove = findDirectoryByPath(currentDirectory, path);
            if (directoryToRemove != null) {
                removeDirectory(directoryToRemove);
            } else {
                System.out.println("File or directory not found");
            }
        }
    }

    // Recursively removes a directory and its contents
    private void removeDirectory(Directory directory) {
        for (File file : directory.files) {
            directory.removeFile(file);
        }
        for (Directory subDir : directory.subDirectories) {
            removeDirectory(subDir);
        }
        Directory parent = directory.getParent();
        if (parent != null) {
            parent.subDirectories.remove(directory);
        }
    }

    // Searches for a string in files within a directory
    private void grepInDirectory(Directory directory, String searchString) {
        boolean found = false;

        for (File file : directory.files) {
            if (file.getContent().contains(searchString)) {
                System.out.println(file.getName() + ": " + file.getContent());
                found = true;
            }
        }

        for (Directory subDir : directory.subDirectories) {
            grepInDirectory(subDir, searchString);
        }

        if (!found) {
            System.out.println("Not found: " + searchString);
        }
    }

    // Save the current state of the file system to a file
    public void saveState(String filePath) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(this);
            System.out.println("File system state saved successfully to " + filePath);
        } catch (IOException e) {
            System.out.println("Error saving file system state: " + e.getMessage());
        }
    }

    // Load the file system state from a file
    public static FileSystem loadState(String filePath) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            FileSystem fileSystem = (FileSystem) ois.readObject();
            System.out.println("File system state loaded successfully from " + filePath);
            return fileSystem;
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error loading file system state: " + e.getMessage());
            return null;
        }
    }
}

// Represents a directory in the file system
class Directory implements Serializable {
    private String name;
    private Directory parent;
    List<Directory> subDirectories;
    List<File> files;

    // Constructor initializes the directory with a name
    public Directory(String name) {
        this.name = name;
        this.subDirectories = new ArrayList<>();
        this.files = new ArrayList<>();
    }

    // Gets the name of the directory
    public String getName() {
        return name;
    }

    // Gets the parent directory
    public Directory getParent() {
        return parent;
    }

    // Sets the parent directory
    public void setParent(Directory parent) {
        this.parent = parent;
    }

    // Adds a subdirectory to the directory
    public void addDirectory(Directory directory) {
        directory.setParent(this);
        subDirectories.add(directory);
    }

    // Adds a file to the directory
    public void addFile(File file) {
        files.add(file);
    }

    // Removes a file from the directory
    public void removeFile(File file) {
        files.remove(file);
    }

    // Gets a subdirectory by name
    public Directory getSubDirectory(String name) {
        for (Directory subDir : subDirectories) {
            if (subDir.getName().equals(name)) {
                return subDir;
            }
        }
        return null;
    }

    // Gets a file by name
    public File getFile(String name) {
        for (File file : files) {
            if (file.getName().equals(name)) {
                return file;
            }
        }
        for (Directory subDir : subDirectories) {
            File file = subDir.getFile(name);
            if (file != null) {
                return file;
            }
        }
        return null;
    }

    // Lists the contents of the directory
    public String listContents() {
        StringBuilder result = new StringBuilder();
        for (Directory dir : subDirectories) {
            result.append(dir.getName()).append("/ ");
        }
        for (File file : files) {
            result.append(file.getName()).append(" ");
        }
        return result.toString().trim();
    }
}

// Represents a file in the file system
class File implements Serializable {
    private String name;
    private String content;

    // Constructor initializes the file with a name
    public File(String name) {
        this.name = name;
        this.content = "";
    }

    // Gets the name of the file
    public String getName() {
        return name;
    }

    // Gets the content of the file
    public String getContent() {
        return content;
    }

    // Sets the content of the file
    public void setContent(String content) {
        this.content = content;
    }

    // Creates a copy of the file
    public File clone() {
        File newFile = new File(this.name);
        newFile.setContent(this.content);
        return newFile;
    }
}

// Entry point for the program
public class InMemoryFileSystem {
    public static void main(String[] args) {
        FileSystem fileSystem;

        // Check if there is a command-line argument for loading the state
        if (args.length == 1 && args[0].equals("load_state")) {
            // Attempt to load the file system state
            fileSystem = FileSystem.loadState("filesystem_state.ser");

            // If loading fails or the saved state doesn't exist, create a new file system
            if (fileSystem == null) {
                System.out.println("Creating a new file system.");
                fileSystem = new FileSystem();
            } else {
                System.out.println("File system state loaded successfully.");
            }
        } else {
            // If no command-line argument is provided, create a new file system
            System.out.println("Creating a new file system.");
            fileSystem = new FileSystem();
        }

        Scanner scanner = new Scanner(System.in);

        // Infinite loop to accept user commands
        while (true) {
            System.out.print(fileSystem.currentDirectory.getName() + "> ");
            String command = scanner.nextLine().trim();
            String[] tokens = command.split("\\s+", 2);

            // Switch statement to execute file system operations based on user input
            switch (tokens[0]) {
                case "mkdir":
                    if (tokens.length == 2) {
                        fileSystem.mkdir(tokens[1]);
                    } else {
                        System.out.println("Usage: mkdir <directory_name>");
                    }
                    break;
                case "cd":
                    if (tokens.length == 2) {
                        fileSystem.cd(tokens[1]);
                        System.out.println("Current directory: " + fileSystem.currentDirectory.getName());
                    } else {
                        System.out.println("Usage: cd <path>");
                    }
                    break;
                case "ls":
                    if (tokens.length <= 2) {
                        fileSystem.ls(tokens.length == 2 ? tokens[1] : null);
                    } else {
                        System.out.println("Usage: ls [path]");
                    }
                    break;
                case "touch":
                    if (tokens.length == 2) {
                        fileSystem.touch(tokens[1]);
                    } else {
                        System.out.println("Usage: touch <file_name>");
                    }
                    break;
                case "echo":
                    if (tokens.length == 2) {
                        String fileName = tokens[1].split("\\s+")[0];
                        String content = command.substring(command.indexOf(" ") + 1 + fileName.length()).trim();
                        fileSystem.echo(fileName, content);
                    } else {
                        System.out.println("Usage: echo <file_name> <content>");
                    }
                    break;
                case "cat":
                    if (tokens.length == 2) {
                        fileSystem.cat(tokens[1]);
                    } else {
                        System.out.println("Usage: cat <file_name>");
                    }
                    break;
                case "grep":
                    if (tokens.length == 2) {
                        fileSystem.grep(tokens[1]);
                    } else {
                        System.out.println("Usage: grep <search_string>");
                    }
                    break;
                case "cp":
                    if (tokens.length == 2) {
                        String[] paths = tokens[1].split("\\s+");
                        if (paths.length == 2) {
                            fileSystem.cp(paths[0], paths[1]);
                        } else {
                            System.out.println("Usage: cp <source_path> <destination_path>");
                        }
                    } else {
                        System.out.println("Usage: cp <source_path> <destination_path>");
                    }
                    break;
                case "mv":
                    if (tokens.length == 2) {
                        String[] paths = tokens[1].split("\\s+");
                        if (paths.length == 2) {
                            fileSystem.mv(paths[0], paths[1]);
                        } else {
                            System.out.println("Usage: mv <source_path> <destination_path>");
                        }
                    } else {
                        System.out.println("Usage: mv <source_path> <destination_path>");
                    }
                    break;
                case "rm":
                    if (tokens.length == 2) {
                        fileSystem.rm(tokens[1]);
                    } else {
                        System.out.println("Usage: rm <path>");
                    }
                    break;
                case "exit":
                    scanner.close();
                    System.exit(0);
                default:
                    System.out.println("Unknown command. Type 'help' for a list of commands.");
            }
        }
    }
}