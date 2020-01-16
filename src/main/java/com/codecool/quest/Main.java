package com.codecool.quest;

import com.codecool.quest.logic.*;
import com.codecool.quest.logic.Items.Items;
import com.codecool.quest.logic.actors.Actor;
import com.codecool.quest.logic.actors.Npc;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;

public class Main extends Application {
    String[] sourceFiles = {"/map.txt", "/map2.txt"};
    GameMap[] levels = new GameMap[sourceFiles.length];
    GameMap map;
    Canvas canvas;
    GraphicsContext context;
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
        this.loadMaps();
        map = levels[0];
        canvas = new Canvas(
                map.getWidth() * Tiles.TILE_WIDTH,
                map.getHeight() * Tiles.TILE_WIDTH);
        context = canvas.getGraphicsContext2D();

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
        inventory.setFocusTraversable(false);

        ui.add(nameInput, 0, 7);
        ui.add(setNameButton, 0, 8);
        setNameButton.setOnAction(value -> {
            map.getPlayer().setName(nameInput.getText());
            canvas.requestFocus();
            nameInput.clear();
            nameInput.setFocusTraversable(false);
            refresh();
        });
        ui.add(playerNameLabel, 0, 9);
        nameInput.setFocusTraversable(false);
        setNameButton.setFocusTraversable(false);
        BorderPane borderPane = new BorderPane();

        borderPane.setCenter(canvas);
        borderPane.setRight(ui);

        Scene scene = new Scene(borderPane);
        primaryStage.setScene(scene);
        refresh();
        scene.setOnKeyPressed(this::onKeyPressed);
        pickButton.setOnAction(this::onPickButtonClick);
        pickButton.setFocusTraversable(false);

        primaryStage.setTitle("Codecool Quest");
        primaryStage.show();
    }

    private void onPickButtonClick(ActionEvent actionEvent) {
        map.getPlayer().pickItem();
        refresh();
    }

    private void onKeyPressed(KeyEvent keyEvent) {
        Cell nextCell;
        switch (keyEvent.getCode()) {
            case UP:
                nextCell = map.getPlayer().move(0, -1);
                changeLevel(nextCell);
                refresh();
                break;
            case DOWN:
                nextCell = map.getPlayer().move(0, 1);
                changeLevel(nextCell);
                refresh();
                break;
            case LEFT:
                nextCell = map.getPlayer().move(-1, 0);
                changeLevel(nextCell);
                refresh();
                break;
            case RIGHT:
                nextCell = map.getPlayer().move(1, 0);
                changeLevel(nextCell);
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
        context.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        int east, west, north, south;
        int charX = map.getPlayer().getX();
        int charY = map.getPlayer().getY();
        int width = map.getWidth();
        int height = map.getHeight();
        int targetCellX = 0;
        int xOffset = 12;
        int yOffset = 10;

        aiMove();
        if (!map.getPlayer().isAlive()) {
            loadMaps();
            map = levels[0];
            map.getPlayer().setHealth(10);
            return;
        }

        context.setFill(Color.BLACK);

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
            int targetCellY = 0;
            for (int y = north; y < south; y++) {

                if (x >= width || x < 0 ||
                        y >= height || y < 0
                ) {
                    Tiles.drawTile(context, new Cell(0, 0, CellType.EMPTY), targetCellX, targetCellY);
                    targetCellY++;
                    continue;
                }
                Cell cell = map.getCell(x, y);
                if (cell.getActor() != null && !cell.getActor().isAlive()) {
                    cell.removeActor();
                }
                if (cell.getActor() != null && cell.getActor().isAlive()) {
                    Tiles.drawTile(context, cell.getActor(), targetCellX, targetCellY);
                } else if (cell.getItem() != null) {
                    Tiles.drawTile(context, cell.getItem(), targetCellX, targetCellY);
                } else if (cell.getDoor() != null) {
                    Tiles.drawTile(context, cell.getDoor(), targetCellX, targetCellY);
                } else if (cell.getStairs() != null) {
                    Tiles.drawTile(context, cell.getStairs(), targetCellX, targetCellY);
                } else if (cell.getDecor() != null) {
                    Tiles.drawTile(context, cell.getDecor(), targetCellX, targetCellY);
                } else {
                    Tiles.drawTile(context, cell, targetCellX, targetCellY);
                }
                targetCellY++;
            }
            targetCellX++;
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
        canvas.requestFocus();
    }

    private void changeLevel(Cell nextCell) {
        if (nextCell != null && nextCell.getStairs() != null) {
            int health = map.getPlayer().getHealth();
            String playerName = map.getPlayer().getName();
            Inventory inventory = map.getPlayer().getInventory();
            int levelTo = nextCell.getStairs().getLevel();
            this.levels[map.getCurrentLevel()] = this.map;
            this.map = this.levels[levelTo];
            this.map.getPlayer().setHealth(health);
            this.map.getPlayer().setInventory(inventory);
            this.map.getPlayer().setName(playerName);
            refresh();
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

    public void loadMaps() {
        for (int i = 0; i < sourceFiles.length; i++) {
            levels[i] = MapLoader.loadMap(sourceFiles[i]);
        }
    }
}
