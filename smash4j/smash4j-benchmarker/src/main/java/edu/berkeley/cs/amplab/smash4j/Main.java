package edu.berkeley.cs.amplab.smash4j;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import edu.berkeley.cs.amplab.vcfparser.Header;
import edu.berkeley.cs.amplab.vcfparser.MetaInformation;
import edu.berkeley.cs.amplab.vcfparser.VcfReader;
import edu.berkeley.cs.amplab.vcfparser.VcfRecord;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.prefs.Preferences;

public class Main {

  static final Preferences
      PREFERENCES = Preferences.userNodeForPackage(Main.class);
  static final String
      PREFERENCES_PATH = PREFERENCES.absolutePath(),
      PROGRAM_NAME = "AMPLab-SMaSH4J/0.1";

  public static void main(String[] args) throws Exception {
    new CommandDispatcher() {

      @Override protected void setPrefs(CommandDispatcher.SetPrefsCommand command) {
        Optional<CommandDispatcher.SetPrefsCommand.AuthorizationMethod>
            authorizationMethod = command.authorizationMethod();
        if (authorizationMethod.isPresent()) {
          PREFERENCES.put("authorizationMethod", authorizationMethod.get().toString());
        }
        Optional<String> apiKey = command.apiKey();
        if (apiKey.isPresent()) {
          PREFERENCES.put("apiKey", apiKey.get());
        }
        Optional<File> clientSecretsFile = command.clientSecretsFile();
        if (clientSecretsFile.isPresent()) {
          PREFERENCES.put("clientSecretsFile", clientSecretsFile.get().getPath());
        }
        Optional<String> serviceAccountId = command.serviceAccountId();
        if (serviceAccountId.isPresent()) {
          PREFERENCES.put("serviceAccountId", serviceAccountId.get());
        }
        Optional<File> serviceAccountP12File = command.serviceAccountP12File();
        if (serviceAccountP12File.isPresent()) {
          PREFERENCES.put("serviceAccountP12File", serviceAccountP12File.get().getPath());
        }
      }

      @Override protected void showPrefs(CommandDispatcher.ShowPrefsCommand command) {
        boolean
            authorizationMethod = command.authorizationMethod(),
            apiKey = command.apiKey(),
            clientSecretsFile = command.clientSecretsFile(),
            serviceAccountId = command.serviceAccountId(),
            serviceAccountP12File = command.serviceAccountP12File(),
            noFlags = !(authorizationMethod
                || apiKey
                || clientSecretsFile
                || serviceAccountId
                || serviceAccountP12File);
        showPref(authorizationMethod || noFlags, "authorizationMethod");
        showPref(apiKey || noFlags, "apiKey");
        showPref(clientSecretsFile || noFlags, "clientSecretsFile");
        showPref(serviceAccountId || noFlags, "serviceAccountId");
        showPref(serviceAccountP12File || noFlags, "serviceAccountP12File");
      }

      private void showPref(boolean condition, String key) {
        if (condition) {
          String value = PREFERENCES.get(key, null);
          System.out.format("%s:%s = %s%n",
              PREFERENCES_PATH, key, null == value ? null : String.format("\"%s\"", value));
        }
      }

      @Override protected void noCommand(String[] args) throws Exception {
        try (Reader in = new FileReader(args[0])) {
          try (final PrintWriter out = new PrintWriter(new FileWriter(args[1]))) {
            VcfReader.from(in).read(
                new VcfReader.Callback<Void>() {
                  @Override
                  public Void readVcf(MetaInformation metaInformation, Header header, FluentIterable<VcfRecord> records) {
                    out.print(metaInformation);
                    out.println(header);
                    for (VcfRecord record : records) {
                      out.println(record);
                    }
                    return null;
                  }
                });
          }
        }
      }
    }.parse(args);
  }
}
