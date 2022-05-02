package cs1302.gallery;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Duration;
import java.net.URL;
import java.net.URLEncoder;
import java.net.MalformedURLException;
import java.io.UnsupportedEncodingException;
import java.io.InputStreamReader;
import java.io.IOException;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;

/**
 * Represents an iTunes GalleryApp.
 */
public class GalleryApp extends Application {

    VBox containAll;
    MenuBar menubar;
    Menu file;
    Menu help;
    MenuItem exit;
    MenuItem about;
    Alert aboutMe;
    ToolBar toolbar;
    Button playPauseButton;
    Separator separate;
    Label searchQuery;
    TextField searchField;
    Button updateImages;
    TilePane albumCovers;
    HBox bottomBorder;
    ProgressBar progressBar;
    Label courtesyMessage;
    boolean isPlayMode;
    String defaultQuery;
    Alert errorAlert;
    ArrayList<String> urlsList;
    ArrayList<ImageView> ivList;

    /**
     * Initializes many of the components contained within the scene.
     * {@inheritDoc}
     */
    @Override
    public void init() {
        isPlayMode = true;

        // initialize the menu bar
        file = new Menu("File");
        exit = new MenuItem("Exit");
        help = new Menu("Help");
        about = new MenuItem("About");
        exit.setOnAction((event) -> System.exit(0));
        file.getItems().add(exit);
        about.setOnAction((event) -> aboutMe.showAndWait());
        help.getItems().add(about);
        menubar = new MenuBar(file, help);

        // initialize the toolbar
        defaultQuery = "Elton John";
        playPauseButton = new Button("Pause");
        playPauseButton.setOnAction(this::playPauseHandler);
        separate = new Separator(Orientation.VERTICAL);
        searchQuery = new Label("Search Query:");
        searchField = new TextField(defaultQuery);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        updateImages = new Button("Update Images");
        updateImages.setOnAction(this::loadImages);
        toolbar = new ToolBar(playPauseButton, separate, searchQuery, searchField, updateImages);

        // initialize progress bar box
        bottomBorder = new HBox(10);
        progressBar = new ProgressBar(0.0);
        courtesyMessage = new Label("Images provided courtesy of iTunes");
        bottomBorder.getChildren().addAll(progressBar, courtesyMessage);

        // initialize the album covers TilePane and lists
        albumCovers = new TilePane();
        albumCovers.setPrefColumns(5);
        urlsList = new ArrayList<String>();
        ivList = new ArrayList<ImageView>();
    } // init

    /**
     * Initializes the scene with all components and displays the app.
     * {@inheritdoc}
     */
    @Override
    public void start(Stage stage) {
        fillUrlsList(defaultQuery);
        for (int i = 0; i < 20; i++) {
            ImageView temp = new ImageView(new Image(urlsList.get(i)));
            ivList.add(temp);
            albumCovers.getChildren().add(temp); // temp addeed to list AND directly to TilePane
            updateProgressBar(0.5 / 20); // updates pb by same amount each iteration of loop
        } // for

     // initialize the VBox, set the scene
        containAll = new VBox();
        containAll.getChildren().addAll(menubar, toolbar, albumCovers, bottomBorder);
        Scene scene = new Scene(containAll);
        stage.setTitle("GalleryApp!");
        stage.setMaxWidth(1280);
        stage.setMaxHeight(720);
        stage.setScene(scene);
        stage.sizeToScene();
        stage.show();

        // initialize errorAlert box, displays if a search does not return 21 or more images
        errorAlert = new Alert(Alert.AlertType.ERROR);
        errorAlert.setTitle("Search Error!");
        errorAlert.setResizable(true);
        errorAlert.setWidth(800.0);
        errorAlert.setHeight(800.0);

        // initialize About Me alert box
        aboutMe = new Alert(Alert.AlertType.INFORMATION);
        aboutMe.setTitle("About Jackson Rupert");
        aboutMe.setResizable(true);
        ImageView aboutMeImage = new ImageView(new Image("file:AboutMe.png"));
        aboutMeImage.setPreserveRatio(true);
        aboutMeImage.setFitHeight(250);
        aboutMe.setHeaderText("");
        aboutMe.setGraphic(aboutMeImage);
        aboutMe.setContentText("Jackson Rupert\njbr35352@uga.edu\nGalleryApp Version 4.2.9\n\n"
                                + "You didn't really read the\nTerms and Conditions, did you?");

        // start the infinite random replacement thread
        runRandomReplacementThread();
    } // start

    /**
     * This method begins an infinite loop, which always takes place in a seperate daemon thread
     * (with the help of {@code runNow} method). The loop traverses each item in the list of URLs,
     * which was previously filled/replaced by the{@code fillUrlsList} method. If the
     * TilePane does not currently show the album cover indicated by the url
     * from the list, that url is used to replace a random album cover in the TilePane,
     * and the scene graph is updated. Once i exceeds or equals {@code urlsList.size() - 1}, i is
     * reset to -1, which will then change to 0 when the loop returns for the next iteration
     * (i.e. the loop is infinite). If the app is in "pause mode"
     * ({@code isPlayMode} is equal to false), random replacement does not occur, i is
     * decreased by 1 so that when the loop returns, it will try that url again until the
     * app is placed back into "play mode" and the replacement occurs. This thread is always running
     * until the app exits, even when the URLs list is changed.
     */
    public void runRandomReplacementThread() {
        runNow(() -> {
            for (int i = 20; i < urlsList.size(); i++) {
                if (isPlayMode) { // random replacement will only occur if app is in play mode
                    String temp = urlsList.get(i);
                    boolean doesContain = false;
                    for (int j = 0; j < ivList.size(); j++) {
                        String getUrlTemp = ivList.get(j).getImage().getUrl();
                        if (getUrlTemp.equals(temp)) {
                            doesContain = true;
                        } // if
                    } // for
                    if (!doesContain) {
                        randomReplacement(urlsList.get(i));
                    } // if
                } else {
                    i--;
                } // if
                if (i >= urlsList.size() - 1) {
                    i = -1;
                } // if
            } // for
        }); // runNow
    } // startRandomReplacementThread

    /**
     * EventHandler for the "Update Images" button.
     * When the button is clicked, the progress bar is reset to 0%, app is temporarily
     * placed into "pause" mode, the pause/play button is disabled, and within a
     * separate daemon thread, the urls list is updated based on the user's
     * search (assuming the search returns enough searches), the scene graph is updated
     * to reflect the new images, and finally the app is placed back into whatever mode
     * it was in when the button was clicked and the play/pause button is reenabled.
     * @param event the ActionEvent generated when the button is clicked
     */
    public void loadImages(ActionEvent event) {
        progressBar.setProgress(0.0);
        playPauseButton.setDisable(true);
        boolean temp = isPlayMode;
        setPlayPauseMode(false);
        runNow(() -> {
            if (fillUrlsList(searchField.getText())) {
                setIVListAndPaneImages();
            } // if
            Platform.runLater(() -> {
                setPlayPauseMode(temp);
                playPauseButton.setDisable(false);
            }); // runLater
        }); // runNow
    } // loadImages

    /**
     * EventHandler for the "Play/Pause" button.
     * When the button is pressed, if the app is currently in "play"
     * mode ({@code isPlayMode} equals false), then app is placed into "pause" mode
     * and the button displays the word "Play," and if the app is currently is "pause" mode,
     * then the app is placed into "play" mode and the button displays the word "Pause".
     * @param event the ActionEvent generated by the button
     */
    public void playPauseHandler(ActionEvent event) {
        if (isPlayMode) {
            setPlayPauseMode(false);
        } else {
            setPlayPauseMode(true);
        } // if
    } // playPauseMode

    /**
     * Method used by other methods to change the current "play/pause" mode.
     * If called with {@code true}, then {@code isPlayPause} is set equal to {@code true}.
     * If called with {@code false}, then {@code isPlayPause} is set equal to {@code false}.
     * @param mode the boolean supplied to the method that determines what mode the app
     * is placed into
     */
    public void setPlayPauseMode(boolean mode) {
        if (mode == true) {
            isPlayMode = true;
            playPauseButton.setText("Pause");
            runRandomReplacementThread();
        } else { // mode == false
            isPlayMode = false;
            playPauseButton.setText("Play");
        } // if
    } // setPlayPause

    /**
     * Updates the list of URLs list based on a given search by the user.
     * Using JSON and Google's Gson, the user's search is applied to an iTunes search
     * query, which returns a long list parsed by Gson. URLs are then placed into the list
     * (no repeat urls are added), the progress bar is updated throughout the search. If less than
     * 21 unique results are returned, then an error message is displayed to the user and the list
     * is not updated.
     * @param query the search supplied by the user used to download the images from iTunes
     * @return true if the list is updated successfully with 21 or more urls
     */
    public boolean fillUrlsList(String query) {
        ArrayList<String> tempList = new ArrayList<>(); // urls stored in a temporary list before
        String newSearch = null;                        // being added to main URLs list
        URL url = null;
        InputStreamReader reader = null;
        try {
            newSearch = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            System.out.println("Your search could not be used! Try again.");
        } // try
        String newURL = "https://itunes.apple.com/search?term=" + newSearch
            + "&limit=200&media=music";
        try {
            url = new URL(newURL);
        } catch (MalformedURLException me) {
            System.out.println("Your search could not be used! Try again.");
        } // try
        try {
            reader = new InputStreamReader(url.openStream());
        } catch (IOException ioe) {
            System.out.println("An error occurred during your search! Try again.");
        } // try
        updateProgressBar(0.1);
        JsonElement je = JsonParser.parseReader(reader);
        JsonObject root = je.getAsJsonObject();
        JsonArray results = root.getAsJsonArray("results");
        int numResults = results.size();
        updateProgressBar(0.1);
        for (int i = 0; i < numResults; i++) {
            JsonObject result = results.get(i).getAsJsonObject();
            JsonElement artworkUrl100 = result.get("artworkUrl100");
            String temp = artworkUrl100.toString();
            String reformat = temp.substring(1, temp.length() - 1);
            if (!tempList.contains(reformat)) {
                tempList.add(reformat);
            } // if
            updateProgressBar(0.3 / numResults);
        } // for
        if (tempList.size() > 20) {
//            System.out.println("LIST SIZE: " + tempList.size());
            urlsList = tempList;
            return true;
        } else {
            Platform.runLater(() -> {
                progressBar.setProgress(0.0);
                if (tempList.size() == 0) { // if 0 results were returned by the query
                    errorAlert.setContentText("Your search \"" + query
                        + "\" did not return any results. Please try again.");
                } else { // if some (but not 21 or more) results were returned
                    errorAlert.setContentText("Your search \"" + query
                        + "\" did not return enough results. Please try again.");
                } // if
                errorAlert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE); // ensures alert
                errorAlert.showAndWait();                                      // window resizes
            });
            return false;
        } // if
    } // fillUrlsList

    /**
     * Method used to change the progress in the progress bar.
     * Used after the update images button is pressed and updates periodically
     * throughout the course of a new iTunes query.
     * @param newProgress the amount by which to update the progress bar
     */
    public void updateProgressBar(double newProgress) {
        Platform.runLater(() -> {
            double currentProgress = progressBar.getProgress();
            progressBar.setProgress(currentProgress + newProgress);
        });
    } // updateProgressBar

    /**
     * Called by the update images button handler after the urls list is successfully
     * updated, updates the image view list based on the urls in the urls list, and
     * adds these updated image views to the TilePane on the scene graph. Updates the progress
     * bar after each new image view is added.
     */
    public void setIVListAndPaneImages() {
        for (int i = 0; i < 20; i++) {
            ivList.get(i).setImage(new Image(urlsList.get(i)));
            ImageView temp = ivList.get(i); // added to both IV list (as a reference for random
            setAlbumCovers(i, temp);        // replacement), and added to the scene graph
            updateProgressBar(0.025); // this method is called when the bar is only at
        } // for                      // 50%, so the remaining 50 is filled from the
    } // setIVListImages              // addition of 20 new images (.5/20 = 0.025)

    /**
     * Used to replace an image on the scene graph after the update images button is
     * pressed and when random replacement occurs.
     * @param i the location in the TilePane where an ImageView is replaced
     * @param temp the ImageView supplied that replace the ImageView at the given index i
     */
    public void setAlbumCovers(int i, ImageView temp) {
        Platform.runLater(() -> {
            albumCovers.getChildren().remove(i); // removes the current image at the given i,
            albumCovers.getChildren().add(i, temp); // replaces it with a new one
        }); // runLater
    } // setAlbumCovers

    /**
     * Called within the infinite runRadomReplacementThread, used to generate a random
     * integer (1-20) that indicates which ImageView on the TilePane will be replaced by a new
     * ImageView that contains a new image based on a url in the urls list. Creates a 2 second time
     * delay in between each random replacement.
     * @param replacement the new url supplied from the urls list that is used to replace the image
     * within an ImageView contained in the iv list and used to replace an image on the scene graph
     */
    public void randomReplacement(String replacement) {
        int index = (int)(Math.random() * 20); // generates a random int from 0 to 20 (exclusive)
        ivList.get(index).setImage(new Image(replacement));
        ImageView temp = ivList.get(index);
        setAlbumCovers(index, temp);
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (java.lang.InterruptedException ie) {
            System.out.println("Time error!");
        }
    } // randomReplacement

    /**
     * Used to quickly begin a new thread, takes in a certain task to generate a new daemon thread.
     * That thread is started automatically by this method from calling the
     * thread's start method.
     * @param task the runnable that contains the code which is executed in the new thread
     */
    public static void runNow(Runnable task) {
        Thread t = new Thread(task);
        t.setDaemon(true); // will not prevent app from closing if not finished
        t.start();
    } // runNow

} // GalleryApp
