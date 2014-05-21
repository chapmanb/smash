package edu.berkeley.cs.amplab.smash4j;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.FileConverter;

import java.io.File;

public abstract class CommandDispatcher {

  @Parameters(separators = "=")
  public static class SetPrefsCommand {

    public enum AuthorizationMethod {
      API_KEY,
      CLIENT_SECRETS,
      SERVICE_ACCOUNT
    }

    public static class AuthorizationMethodConverter
        implements IStringConverter<AuthorizationMethod> {
      @Override public AuthorizationMethod convert(String name) {
        return AuthorizationMethod.valueOf(name);
      }
    }

    public static class FileValidator implements IValueValidator<File> {

      private abstract static class Test {

        private final String reason;

        Test(String reason) {
          this.reason = reason;
        }

        abstract boolean test(File file);

        final void test(String flag, File file) throws ParameterException {
          if (!test(file)) {
            throw new ParameterException(
                String.format("File \"%s\" specified in the \"%s\" flag %s", file, flag, reason));
          }
        }
      }

      private static final Test[] TESTS = {
          new Test("doesn't exist") {
            boolean test(File file) {
              return file.exists();
            }
          },
          new Test("is not a file") {
            boolean test(File file) {
              return file.isFile();
            }
          },
          new Test("is not readable") {
            boolean test(File file) {
              return file.canRead();
            }
          }
      };

      @Override public void validate(String flag, File file) throws ParameterException {
        for (Test test : TESTS) {
          test.test(flag, file);
        }
      }
    }

    @Parameter(
        names = { "--authorizationMethod" },
        converter = AuthorizationMethodConverter.class)
    private AuthorizationMethod authorizationMethod;

    @Parameter(
        names = { "--apiKey" })
    private String apiKey;

    @Parameter(
        names = { "--clientSecretsFile" },
        converter = FileConverter.class,
        validateValueWith = FileValidator.class)
    private File clientSecretsFile;

    @Parameter(
        names = { "--serviceAccountId" })
    private String serviceAccountId;

    @Parameter(
        names = { "--serviceAccountP12File" },
        converter = FileConverter.class,
        validateValueWith = FileValidator.class)
    private File serviceAccountP12File;

    public AuthorizationMethod authorizationMethod() {
      return authorizationMethod;
    }

    public String apiKey() {
      return apiKey;
    }

    public File clientSecretsFile() {
      return clientSecretsFile;
    }

    public String serviceAccountId() {
      return serviceAccountId;
    }

    public File serviceAccountP12File() {
      return serviceAccountP12File;
    }
  }

  public static class ShowPrefsCommand {

    @Parameter(names = { "--authorizationMethod" })
    private boolean authorizationMethod;

    @Parameter(names = { "--apiKey" })
    private boolean apiKey;

    @Parameter(names = { "--clientSecretsFile" })
    private boolean clientSecretsFile;

    @Parameter(names = { "--serviceAccountId" })
    private boolean serviceAccountId;

    @Parameter(names = { "--serviceAccountP12File" })
    private boolean serviceAccountP12File;

    public boolean authorizationMethod() {
      return authorizationMethod;
    }

    public boolean apiKey() {
      return apiKey;
    }

    public boolean clientSecretsFile() {
      return clientSecretsFile;
    }

    public boolean serviceAccountId() {
      return serviceAccountId;
    }

    public boolean serviceAccountP12File() {
      return serviceAccountP12File;
    }
  }

  protected abstract void setPrefs(SetPrefsCommand command) throws Exception;
  protected abstract void showPrefs(ShowPrefsCommand command) throws Exception;
  protected abstract void noCommand() throws Exception;

  public final void parse(String... args) throws Exception {
    JCommander jCommander = new JCommander();
    jCommander.setProgramName("smash4j");
    SetPrefsCommand setPrefs = new SetPrefsCommand();
    ShowPrefsCommand showPrefs = new ShowPrefsCommand();
    jCommander.addCommand("setprefs", setPrefs);
    jCommander.addCommand("showprefs", showPrefs);
    jCommander.parse(args);
    String command = jCommander.getParsedCommand();
    if (null != command) {
      switch (command) {
        case "setprefs":
          setPrefs(setPrefs);
          break;
        case "showprefs":
          showPrefs(showPrefs);
          break;
        default:
          throw new IllegalStateException(String.format("Unrecognized command: \"%s\"", command));
      }
    } else {
      noCommand();
    }
  }
}
