package com.mycompany.app;

/**
 This can be run with the following commands:
 mvn package 
 mvn exec:java -D exec.mainClass=com.mycompany.app.App
 $env:GOOGLE_APPLICATION_CREDENTIALS="credentials.json"
 */
import java.awt.*;
import java.awt.event.*;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.swing.JTable;

import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.dataproc.Dataproc;
import com.google.api.services.dataproc.model.Job;
import com.google.api.services.dataproc.model.JobPlacement;
import com.google.api.services.dataproc.model.JobReference;
import com.google.api.services.dataproc.model.SparkJob;
import com.google.api.services.dataproc.model.SubmitJobRequest;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.storage.Blob.BlobSourceOption;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import okhttp3.*;

public class App extends Frame implements ActionListener {
  TextField tf;
  Label l1, l2;
  ArrayList<Label> rows;
  Button b;
  private final OkHttpClient httpClient = new OkHttpClient();

  App() {
    rows = new ArrayList<Label>();
    // create components
    tf = new TextField();
    l1 = new Label("Enter search term:");
    l2 = new Label("");
    l1.setBounds(30, 60, 240, 30);
    l1.setAlignment(Label.CENTER);
    l2.setBounds(30, 160, 240, 30);
    tf.setBounds(30, 90, 270, 30);
    b = new Button("Search");
    b.setBounds(100, 130, 80, 30);

    // register listener
    b.addActionListener(this);// passing current instance

    // add components and set size, layout and visibility
    add(b);
    add(tf);
    add(l1);
    add(l2);
    setSize(300, 400);
    setLayout(null);
    setVisible(true);
  }

  public void addToOutput(String output) {
    Label myLabel = new Label(output);
    myLabel.setBounds(30, 200 + (rows.size() * 40), 240, 30);
    rows.add(myLabel); // Saves a reference to global var
    add(myLabel);
  }

  public void actionPerformed(ActionEvent e) {
    // Connect to Google Cloud Platform API
    String term = tf.getText().toLowerCase().trim();
    if (term.equals("")) {
      l2.setText("Please enter input first.");
    }
    System.out.println("Submitting job...");
    GoogleCredentials credentials;
    try {
      credentials = GoogleCredentials
          .fromStream(new FileInputStream("../temp-credentials.json"))
          //.fromStream(new FileInputStream("C:\\Users\\mattc\\Desktop\\Cloud-Final-Project\\credentials.json"))
          .createScoped(Lists.newArrayList("https://www.googleapis.com/auth/cloud-platform"));
    } catch (Exception ex) {
      System.out.println("Could not connect to the google cloud");
      System.out.println(ex.toString());
      System.exit(-1);
      return;
    }
    HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);
    Dataproc dataproc = new Dataproc.Builder(new NetHttpTransport(), new JacksonFactory(), requestInitializer)
        .setApplicationName("Java Gui Program").build();

    String projectId = "my-project-1488228972103";
    Random rand = new Random();
    int randNum = rand.nextInt(10000);
    String curJobId = "json-agg-job-" + UUID.randomUUID().toString();
    Job jobSnapshot = null;

    try {
      jobSnapshot = dataproc.projects().regions().jobs()
          .submit(projectId, "us-west1",
              new SubmitJobRequest().setJob(new Job().setReference(new JobReference().setJobId(curJobId))
                  .setPlacement(new JobPlacement().setClusterName("hadoop-cluster-1"))
                  .setSparkJob(new SparkJob().setMainClass("WordCount")
                      .setJarFileUris(
                          ImmutableList.of("gs://dataproc-staging-us-west1-86078174858-m1xucgmy/Jar/CloudProject.jar"))
                      .setArgs(ImmutableList.of("gs://dataproc-staging-us-west1-86078174858-m1xucgmy/Data/", // Input
                          "gs://dataproc-staging-us-west1-86078174858-m1xucgmy/Output" + randNum // Output
                      ))))).execute();
    } catch (Exception ex) {
      System.out.println("The job failed!");
      System.out.println(ex.toString());
      System.exit(-1);
      return;
    }

    System.out.println("Job successfully submitted!  Please wait 45 seconds");
    l2.setText("Processing job, please wait 45s");
    int sec = 1;
    while (true) {
      try {
        Thread.sleep(1000);
      } catch (Exception ex) {}
      System.out.println("Running... " + sec + "s");
      if (sec >= 45) { break; }
      sec += 1;
    }
    System.out.println("Done!");
    
    System.out.println("Getting bucket 'dataproc-staging-us-west1-86078174858-m1xucgmy/Output" + randNum + "'...");
    Storage storage = StorageOptions.newBuilder().setCredentials(credentials)
      .setProjectId(projectId).build().getService();
    
    HashMap<String, ArrayList<String>> map = new HashMap<String, ArrayList<String>>();
    for (int outputNum = 0; outputNum <= 10; outputNum += 1) {
      String filePath = "Output" + randNum + "/part-r-0000" + outputNum;
      if (outputNum == 10) {
        filePath = "Output" + randNum + "/part-r-00010";
      }
      Blob blob = storage.get(BlobId.of("dataproc-staging-us-west1-86078174858-m1xucgmy", filePath));
      byte[] content = blob.getContent();
      String[] lines = (new String(content)).split("\n");  
      for (String line : lines) {
        String[] tokens = line.split("\\s+");
        String val = tokens[1] + " " + tokens[2];
        if (map.containsKey(tokens[0])) {
          ArrayList<String> docArr = map.get(tokens[0]);
          docArr.add(val);
          map.put(tokens[0], docArr);
        } else {
          ArrayList<String> docArr = new ArrayList<String>();
          docArr.add(val);
          map.put(tokens[0], docArr);
        }
      }
      System.out.println("Outputting file " + outputNum + " - " + lines.length + " lines");
    }

    System.out.println("Processed " + map.size() + " terms into a hash map!");
    ArrayList<String> docArr = map.get(term);
    System.out.println("Found term " + term + " in " + docArr.size() + " docs!");    
    l2.setText("Results for '" + term + "': (in " + docArr.size() + " docs)");
    for (String doc : docArr) {
      String[] tokens = doc.split(" ");
      addToOutput(tokens[1] + " " + tokens[0]);
    }
    
  }  
  
  public static void main(String args[]){  
    new App();  
  }  
}  