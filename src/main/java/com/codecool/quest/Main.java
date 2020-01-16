package com.codecool.quest;

import com.codecool.quest.logic.Cell;
import com.codecool.quest.logic.CellType;
import com.codecool.quest.logic.GameMap;
import com.codecool.quest.logic.Items.Items;
import com.codecool.quest.logic.MapLoader;
import com.codecool.quest.logic.actors.Actor;
import com.codecool.quest.logic.actors.Npc;
import com.codecool.quest.logic.actors.Player;
import com.codecool.quest.logic.actors.Skeleton;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;


import java.awt.*;

import java.util.ArrayList;

public class Main extends Application {
    GameMap level1 = MapLoader.loadMap("/map.txt");
    GameMap level2 = MapLoader.loadMap("/map2.txt");
    GameMap map = level1;
    Canvas canvas = new Canvas(
            map.getWidth() * Tiles.TILE_WIDTH,
            map.getHeight() * Tiles.TILE_WIDTH);
    GraphicsContext context = canvas.getGraphicsContext2D();
    Label healthLabel = new Label();
    ListView<String> inventory = new ListView<>();
    Button pickButton = new Button("Pick up");
    Label playerNameLabel = new Label();
    TextField nameInput = new TextField();
    Button setNameButton = new Button("Set Name");

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        GridPane ui = new GridPane();
        ui.setPrefWidth(210);
        ui.setPadding(new Insets(10));
        ui.setVgap(10);

        ui.add(new Label("Health: "), 0, 0);
        ui.add(healthLabel, 1, 0);
        ui.add(new Label("Inventory:"), 0, 2);
        ui.add(pickButton, 0, 6, 2, 1);

        pickButton.setDisable(true);

        ui.add(inventory, 0, 3, 2, 1);
        inventory.setOnKeyPressed(this::onKeyPressed);

        ui.add(nameInput, 0, 7);
        nameInput.setOnKeyPressed(this::onKeyPressed);
        ui.add(setNameButton, 0, 8);
        setNameButton.setOnAction(value -> {
            map.getPlayer().setName(nameInput.getText());
            ui.requestFocus();
            refresh();
        });
        setNameButton.setOnKeyPressed(this::onKeyPressed);
        ui.add(playerNameLabel, 0, 9);

        BorderPane borderPane = new BorderPane();

        borderPane.setCenter(canvas);
        borderPane.setRight(ui);

        Scene scene = new Scene(borderPane);
        primaryStage.setScene(scene);
        refresh();
        scene.setOnKeyPressed(this::onKeyPressed);
        pickButton.setOnAction(this::onPickButtonClick);

        primaryStage.setTitle("Codecool Quest");
        primaryStage.show();
    }

    private void onPickButtonClick(ActionEvent actionEvent) {
        map.getPlayer().pickItem();
        refresh();
    }

    private void onKeyPressed(KeyEvent keyEvent) {
        switch (keyEvent.getCode()) {
            case UP:
                map.getPlayer().move(0, -1);
                refresh();
                break;
            case DOWN:
                map.getPlayer().move(0, 1);
                refresh();
                break;
            case LEFT:
                map.getPlayer().move(-1, 0);
                refresh();
                break;
            case RIGHT:
                map.getPlayer().move(1, 0);
                refresh();
                break;
            case ENTER:
                if (map.getPlayer().getCell().getItem() != null) {
                    map.getPlayer().pickItem();
                    refresh();
                }
                break;
            case SHIFT:
                if (map.getPlayer().getInventory().contains("turkey leg")) {
                    map.getPlayer().getInventory().removeItemByItemName("turkey leg");
                    map.getPlayer().changeHealth(5);
                    refresh();
                }
                break;
        }
    }

    private void refresh() {
        aiMove();
        if (!map.getPlayer().isAlive()) {
            map = MapLoader.loadMap("/map.txt");
            map.getPlayer().setHealth(10);
            return;
        }

        context.setFill(Color.BLACK);
        context.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        int charX = map.getPlayer().getX();
        int charY = map.getPlayer().getY();
        int width = map.getWidth();
        int height = map.getHeight();
        int tCellX = 0;
        int xOffset = 12;
        int yOffset = 10;
        int east, west, north, south;
        if (height > 20 || width > 25) {
            north = charY - yOffset;
            west = charX - xOffset - 1;
            south = charY + yOffset;
            east = charX + xOffset + 1;
        } else {
            north = 0;
            west = 0;
            south = height;
            east = width;
        }

        for (int x = west; x < east; x++) {
            int tCellY = 0;
            for (int y = north; y < south; y++) {

                if (x >= width || x < 0 ||
                        y >= height || y < 0
                ) {
                    Tiles.drawTile(context, new Cell(0, 0, CellType.EMPTY), tCellX, tCellY);
                    tCellY++;
                    continue;
                }
                Cell cell = map.getCell(x, y);
                if (cell.getActor() != null && !cell.getActor().isAlive()) {
                    cell.removeActor();
                }
                if (cell.getActor() != null && cell.getActor().isAlive()) {
                    Tiles.drawTile(context, cell.getActor(), tCellX, tCellY);
                } else if (cell.getItem() != null) {
                    Tiles.drawTile(context, cell.getItem(), tCellX, tCellY);
                } else if (cell.getDoor() != null) {
                    Tiles.drawTile(context, cell.getDoor(), tCellX, tCellY);
                } else if (cell.getDecor() != null) {
                    Tiles.drawTile(context, cell.getDecor(), tCellX, tCellY);
                } else {
                    Tiles.drawTile(context, cell, tCellX, tCellY);
                }
                tCellY++;
            }
            tCellX++;
        }
        playerNameLabel.setText("Name: " + map.getPlayer().getName());
        inventory.getItems().clear();
        for (Items item : map.getPlayer().getInventory().getItems()) {
            inventory.getItems().add(item.getTileName());
        }
        healthLabel.setText("" + map.getPlayer().getHealth());

        if (map.getPlayer().getCell().getItem() != null) {
            pickButton.setText("Pick up " + map.getPlayer().getCell().getItem().getTileName());
            pickButton.setDisable(false);

        } else {
            pickButton.setDisable(true);
            pickButton.setText("Pick up");
        }
    }

    private void aiMove() {
        ArrayList<Actor> actors = new ArrayList<>();
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                Cell cell = map.getCell(x, y);
                if (cell.getActor() instanceof Npc && cell.getActor().isAlive()) {
                    actors.add(cell.getActor());
                }
            }
        }
        for (Actor actor : actors) {
            actor.moveAi();
        }
    }

}
