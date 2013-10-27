/**
 * This work is made available under the Apache License, Version 2.0.
 *
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.pcbje.ahbm;

import com.pcbje.ahbm.matchable.Matchable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;
import org.openide.util.Exceptions;
import org.sleuthkit.autopsy.casemodule.Case;
import org.sleuthkit.autopsy.ingest.IngestMessage;
import org.sleuthkit.autopsy.ingest.IngestServices;
import org.sleuthkit.datamodel.Content;
import org.sleuthkit.datamodel.TskCoreException;

/**
 *
 * @author pcbje
 */
public class CaseWrapper {

    private static final int AHBM_MAX_FILE_SIZE = 64;
    private static final int READ_BUFFER_SIZE = 1024;
    private static final String DEFAULT_SKIP_KNOWN_GOOD_FILES = "false";
    private static final String DEFAULT_AGAINST_EXISTING = "false";
    private volatile int messageID = 0;

    public File getFileInModuleDir(String pathRelativeToModule) {
        StringBuilder path = new StringBuilder();
        path.append(Case.getCurrentCase().getModulesOutputDirAbsPath());
        path.append(File.separator);
        path.append("AHBM");
        path.append(File.separator);
        path.append(pathRelativeToModule);

        File file = new File(path.toString());
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        return file;
    }

    public Content getContentById(long objId) throws TskCoreException {
        return Case.getCurrentCase().getSleuthkitCase().getContentById(objId);
    }

    public void addOverizeWarning(Content content) throws TskCoreException {
        IngestServices.getDefault().postMessage(IngestMessage.
                createMessage(messageID++, IngestMessage.MessageType.WARNING, AhbmIngestModule.getDefault(),
                String.format("File %s is too large for AHBM", content.getUniquePath())));
    }

    public void addMatchNotice(Matchable probe) throws TskCoreException {
        String name = probe.getContent().getName();

        IngestServices.getDefault().postMessage(IngestMessage.
                createMessage(messageID++, IngestMessage.MessageType.INFO, AhbmIngestModule.getDefault(),
                String.format("Found AHBM hits for %s", name)));
    }

    public Properties getProperties() {
        Properties props = new Properties();

        props.setProperty("ahbm.max.file.size", Integer.toString(AHBM_MAX_FILE_SIZE));
        props.setProperty("read.buffer.size", Integer.toString(READ_BUFFER_SIZE));
        props.setProperty("ahbm.skip.known.good", DEFAULT_SKIP_KNOWN_GOOD_FILES);
        props.setProperty("ahbm.against.existing", DEFAULT_AGAINST_EXISTING);

        try {
            props.load(new FileInputStream(getFileInModuleDir("ahbm.properties")));
        } catch (FileNotFoundException ex) {
            storeProperties(props);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        return props;
    }

    public void readFile(OutputStream baos, int bufferSize, Content content) {

        byte[] buffer = new byte[bufferSize];

        try {
            for (long pos = 0; pos < content.getSize(); pos = pos + buffer.length) {
                int remaining = (int) Math.min(content.getSize() - pos, buffer.length);

                int read = content.read(buffer, pos, remaining);

                baos.write(buffer, 0, read);
            }
        } catch (Exception e) {
            Exceptions.printStackTrace(e);
        }
    }

    public void storeProperties(Properties properties) {
        try {
            properties.store(new FileOutputStream(getFileInModuleDir("ahbm.properties")), null);
        } catch (IOException ex1) {
            Exceptions.printStackTrace(ex1);
        }
    }
}
