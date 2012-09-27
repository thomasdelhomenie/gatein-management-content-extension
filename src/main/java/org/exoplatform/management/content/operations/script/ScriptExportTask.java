package org.exoplatform.management.content.operations.script;

import java.io.IOException;
import java.io.OutputStream;

import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class ScriptExportTask implements ExportTask {

  private String scriptPath;
  private String scriptData;

  public ScriptExportTask(String scriptPath, String scriptData) {
    this.scriptPath = scriptPath;
    this.scriptData = scriptData;
  }

  @Override
  public String getEntry() {
    return scriptPath;
  }

  @Override
  public void export(OutputStream outputStream) throws IOException {
    try {
      outputStream.write(scriptData.getBytes("UTF-8"));
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

}