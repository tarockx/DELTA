package delta.desktoptools.library;

/**
 * Created by Elia on 26/05/2015.
 */
public interface ProgressReportListener {
    public void ReportProgress(String data);
    public void ReportError(String data);
    public void Finished(boolean success);
}
