package app.batchdownloader;


public class FileInfo {
	private String filename;
	private String filepath;
	private String error = "";

	public FileInfo(String path, String name) {
		filepath = path;
		filename = name;
	}

	public String getFilename() {
		return filename;
	}

	public String getFilepath() {
		return filepath;
	}

	public String getError() {
		return error;
	}

	public void setError(String string) {
		error = string;
	}
}