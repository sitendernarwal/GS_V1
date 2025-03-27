package com.sitender.gs_v1;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import android.graphics.pdf.PdfDocument;
import java.io.IOException;
public class MainActivity extends AppCompatActivity {
    private DrawingView drawingView;
    private TextView zoomPercentage;
    private SeekBar zoomSeekBar;
    private List<Map<String, Object>> pages = new ArrayList<>();
    private int currentPageIndex = 0;
    private float currentZoom = 1.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        drawingView = findViewById(R.id.drawingView);
        zoomPercentage = findViewById(R.id.zoomPercentage);
        zoomSeekBar = findViewById(R.id.zoomSeekBar);

        // Set up tool buttons
        ImageButton penButton = findViewById(R.id.penButton);
        ImageButton pencilButton = findViewById(R.id.pencilButton);
        ImageButton textButton = findViewById(R.id.textButton);
        ImageButton eraserButton = findViewById(R.id.eraserButton);
        ImageButton brushButton = findViewById(R.id.brushButton);
        ImageButton gridButton = findViewById(R.id.gridButton);

        // Set up color buttons
        Button colorBlue = findViewById(R.id.colorBlue);
        Button colorPurple = findViewById(R.id.colorPurple);
        Button colorCyan = findViewById(R.id.colorCyan);
        Button colorRed = findViewById(R.id.colorRed);
        Button colorGreen = findViewById(R.id.colorGreen);
        Button colorOrange = findViewById(R.id.colorOrange);
        Button colorMagenta = findViewById(R.id.colorMagenta);
        Button colorBlack = findViewById(R.id.colorBlack);
        Button addColorButton = findViewById(R.id.addColorButton);

        // Set up control buttons
        ImageButton layersButton = findViewById(R.id.layersButton);
        ImageButton undoButton = findViewById(R.id.undoButton);
        ImageButton redoButton = findViewById(R.id.redoButton);
        ImageButton zoomOutButton = findViewById(R.id.zoomOutButton);
        ImageButton zoomInButton = findViewById(R.id.zoomInButton);
        ImageButton backButton = findViewById(R.id.backButton);
        ImageButton shareButton = findViewById(R.id.shareButton);
        ImageButton menuButton = findViewById(R.id.menuButton);
        ImageButton prevPageButton = findViewById(R.id.prevPageButton);
        ImageButton nextPageButton = findViewById(R.id.nextPageButton);

        // Set up placeholder text
        TextView placeholderText = findViewById(R.id.placeholderText);

        // Set initial selected tool
        penButton.setSelected(true);
        pages.add(drawingView.getDrawingData());
        currentPageIndex = 0;
        // Set up menu button
        menuButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Menu");
            String[] options = {"New Drawing", "Clear Canvas", "Export Current Page as Image", "Export All Pages as PDF", "Settings"};
            builder.setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: // New Drawing
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("New Drawing")
                                .setMessage("Are you sure you want to start a new drawing? All unsaved work will be lost.")
                                .setPositiveButton("Yes", (dialogInterface, i) -> {
                                    drawingView.clearCanvas();
                                    drawingView.resetTranslation();
                                    currentZoom = 1.0f;
                                    updateZoom();
                                    pages.clear();
                                    pages.add(drawingView.getDrawingData());
                                    currentPageIndex = 0;
                                })
                                .setNegativeButton("No", null)
                                .show();
                        break;
                    case 1: // Clear Canvas
                        drawingView.clearCanvas();
                        break;
                    case 2: // Export Current Page as Image
                        exportAsImage();
                        break;
                    case 3: // Export All Pages as PDF
                        exportAllPagesAsPDF();
                        break;
                    case 4: // Settings
                        showSettings();
                        break;
                }
            });
            builder.show();
        });
        // Set up click listeners for tool buttons
        penButton.setOnClickListener(v -> {
            resetToolButtons();
            penButton.setSelected(true);
            drawingView.setTool(DrawingView.ToolType.PEN);
        });

        pencilButton.setOnClickListener(v -> {
            resetToolButtons();
            pencilButton.setSelected(true);
            drawingView.setTool(DrawingView.ToolType.PENCIL);
        });

        textButton.setOnClickListener(v -> {
            resetToolButtons();
            textButton.setSelected(true);
            drawingView.setTool(DrawingView.ToolType.TEXT);
        });

        eraserButton.setOnClickListener(v -> {
            resetToolButtons();
            eraserButton.setSelected(true);
            drawingView.enableEraser();
        });

        brushButton.setOnClickListener(v -> {
            resetToolButtons();
            brushButton.setSelected(true);

            // Create a simple dialog with a seek bar for brush size
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Brush Size");

            // Setup a seekbar for brush size
            SeekBar brushSizeSeekBar = new SeekBar(MainActivity.this);
            brushSizeSeekBar.setMax(50);
            brushSizeSeekBar.setProgress((int)drawingView.getBrushSize());

            builder.setView(brushSizeSeekBar);
            builder.setPositiveButton("OK", (dialog, which) -> {
                drawingView.setBrushSize(brushSizeSeekBar.getProgress());
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        });

        gridButton.setOnClickListener(v -> {
            resetToolButtons();
            gridButton.setSelected(true);
            drawingView.toggleGrid();
        });

        // Set up click listeners for color buttons
        colorBlue.setOnClickListener(v -> drawingView.setBrushColor(Color.BLUE));
        colorPurple.setOnClickListener(v -> drawingView.setBrushColor(Color.parseColor("#800080")));
        colorCyan.setOnClickListener(v -> drawingView.setBrushColor(Color.CYAN));
        colorRed.setOnClickListener(v -> drawingView.setBrushColor(Color.RED));
        colorGreen.setOnClickListener(v -> drawingView.setBrushColor(Color.GREEN));
        colorOrange.setOnClickListener(v -> drawingView.setBrushColor(Color.parseColor("#FFA500")));
        colorMagenta.setOnClickListener(v -> drawingView.setBrushColor(Color.MAGENTA));
        colorBlack.setOnClickListener(v -> drawingView.setBrushColor(Color.BLACK));

        // Set up add color button
        addColorButton.setOnClickListener(v -> {
            // Create a simple color picker dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Select Custom Color");

            // Create a simple color grid
            String[] colorOptions = {"Red", "Green", "Blue", "Yellow", "Magenta", "Cyan",
                    "Maroon", "Dark Green", "Navy Blue", "Olive", "Purple", "Teal",
                    "Orange", "Lime", "Spring Green", "Sky Blue", "Violet", "Rose"};
            String[] colorHex = {"#FF0000", "#00FF00", "#0000FF", "#FFFF00", "#FF00FF", "#00FFFF",
                    "#800000", "#008000", "#000080", "#808000", "#800080", "#008080",
                    "#FF8000", "#80FF00", "#00FF80", "#0080FF", "#8000FF", "#FF0080"};

            ListView colorListView = new ListView(MainActivity.this);
            ArrayAdapter<String> colorAdapter = new ArrayAdapter<>(MainActivity.this,
                    android.R.layout.simple_list_item_1, colorOptions);
            colorListView.setAdapter(colorAdapter);

            colorListView.setOnItemClickListener((parent, view, position, id) -> {
                int color = Color.parseColor(colorHex[position]);
                drawingView.setBrushColor(color);
                // Close dialog
            });

            builder.setView(colorListView);
            builder.setNegativeButton("Cancel", null);
            AlertDialog dialog = builder.create();
            dialog.show();
        });

        // Set up layers button
        layersButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Layers");

            List<String> layerNames = drawingView.getLayerNames();
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    MainActivity.this,
                    android.R.layout.simple_list_item_1,
                    layerNames
            );

            ListView listView = new ListView(MainActivity.this);
            listView.setAdapter(adapter);

            // Handle layer selection
            listView.setOnItemClickListener((parent, view, position, id) -> {
                drawingView.setCurrentLayer(position);
            });

            // Handle long-press to delete
            listView.setOnItemLongClickListener((parent, view, position, id) -> {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete Layer")
                        .setMessage("Delete this layer?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            if (drawingView.getLayerNames().size() > 1) {
                                drawingView.removeCurrentLayer(); // Remove the current layer
                                layerNames.remove(position); // Update the list
                                adapter.notifyDataSetChanged(); // Refresh UI
                            } else {
                                Toast.makeText(
                                        MainActivity.this,
                                        "You must have at least one layer",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return true;
            });

            builder.setView(listView);
            builder.setPositiveButton("Add Layer", (dialog, which) -> {
                drawingView.addLayer("Layer " + (layerNames.size() + 1));
            });
            builder.setNegativeButton("Close", null);
            builder.show();
        });

        // Set up undo and redo buttons
        undoButton.setOnClickListener(v -> drawingView.undo());
        redoButton.setOnClickListener(v -> drawingView.redo());

        // Set up zoom controls
        zoomInButton.setOnClickListener(v -> {
            currentZoom += 0.1f;
            updateZoom();
        });

        zoomOutButton.setOnClickListener(v -> {
            currentZoom -= 0.1f;
            updateZoom();
        });

        prevPageButton.setOnClickListener(v -> {
            if (currentPageIndex > 0) {
                switchToPage(currentPageIndex - 1);
            }
        });

        nextPageButton.setOnClickListener(v -> {
            if (currentPageIndex < pages.size() - 1) {
                switchToPage(currentPageIndex + 1);
            } else {
                addNewPage();
            }
        });
        // Set up zoom seekbar
        zoomSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    currentZoom = progress / 100f;
                    updateZoom();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Not needed
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Not needed
            }
        });

        // Set up back button
        backButton.setOnClickListener(v -> onBackPressed());

        // Set up share button
        shareButton.setOnClickListener(v -> shareDrawing());

        // When drawing starts, hide the placeholder text
        drawingView.setOnTouchListener((v, event) -> {
            placeholderText.setVisibility(View.GONE);
            return false; // Allow the touch event to be passed to the drawing view
        });

        // Initialize the zoom display
        updateZoom();
    }
    private void addNewPage() {
        // Save the current page state
        if (currentPageIndex < pages.size()) {
            pages.set(currentPageIndex, drawingView.getDrawingData());
        }
        // Clear the canvas for a new page
        drawingView.clearCanvas();
        drawingView.resetTranslation();
        // Add the new page state
        pages.add(drawingView.getDrawingData());
        currentPageIndex = pages.size() - 1;
    }

    private void switchToPage(int index) {
        if (index < 0 || index >= pages.size()) return;
        // Save the current page state
        pages.set(currentPageIndex, drawingView.getDrawingData());
        currentPageIndex = index;
        // Load the selected page
        drawingView.setDrawingData(pages.get(currentPageIndex));
    }
    private void resetToolButtons() {
        ImageButton penButton = findViewById(R.id.penButton);
        ImageButton pencilButton = findViewById(R.id.pencilButton);
        ImageButton textButton = findViewById(R.id.textButton);
        ImageButton eraserButton = findViewById(R.id.eraserButton);
        ImageButton brushButton = findViewById(R.id.brushButton);
        ImageButton gridButton = findViewById(R.id.gridButton);

        // Reset selection state
        penButton.setSelected(false);
        pencilButton.setSelected(false);
        textButton.setSelected(false);
        eraserButton.setSelected(false);
        brushButton.setSelected(false);
        gridButton.setSelected(false);
    }

    private void updateZoom() {
        // Limit zoom between 50% and 300%
        currentZoom = Math.max(0.5f, Math.min(currentZoom, 3.0f));

        // Update the zoom percentage display
        int zoomPercent = (int)(currentZoom * 100);
        zoomPercentage.setText(zoomPercent + "%");

        // Update the seek bar if it's not what triggered this update
        if (zoomSeekBar.getProgress() != zoomPercent) {
            zoomSeekBar.setProgress(zoomPercent);
        }

        // Apply zoom to the drawing view
        drawingView.setZoomLevel(currentZoom);
    }

    // Create a bitmap of the current drawing at full size
    private Bitmap createBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(
                drawingView.getWidth(),
                drawingView.getHeight(),
                Bitmap.Config.ARGB_8888
        );
        Canvas canvas = new Canvas(bitmap);
        drawingView.draw(canvas);
        return bitmap;
    }

    // Method to create a thumbnail of the current drawing
    private Bitmap createThumbnail() {
        if (drawingView.getWidth() == 0 || drawingView.getHeight() == 0) {
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Fallback
        }
        Bitmap bitmap = createBitmap();
        // Scale to a smaller size, e.g., 100x100
        return Bitmap.createScaledBitmap(bitmap, 100, 100, true);
    }

    // Method to export the drawing as an image
    private void exportAsImage() {
        // Create a bitmap of the current drawing at full size
        Bitmap bitmap = createBitmap();

        // Save the bitmap to a file in the Pictures directory
        String fileName = "drawing_" + System.currentTimeMillis() + ".webp";
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), fileName);

        try {
            FileOutputStream out = new FileOutputStream(file);
            // Use WEBP lossless compression for maximum quality and optimized size
            bitmap.compress(Bitmap.CompressFormat.WEBP, 100, out); // 100 = lossless
            out.flush();
            out.close();

            // Notify the user that the image has been saved
            Toast.makeText(this, "Drawing saved to " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();

            // Notify the media scanner to make the image available in the gallery
            MediaScannerConnection.scanFile(this, new String[]{file.getAbsolutePath()}, null, null);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save drawing", Toast.LENGTH_SHORT).show();
        } finally {
            bitmap.recycle(); // Free up bitmap memory
        }
    }

    // Method to share the drawing
    private void shareDrawing() {
        // Create a bitmap of the current drawing
        Bitmap bitmap = createThumbnail();

        // Save the bitmap to a file
        File file = new File(getExternalCacheDir(), "drawing_to_share.png");
        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save drawing for sharing", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a share intent
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/png");
        Uri fileUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // Start the share activity
        startActivity(Intent.createChooser(shareIntent, "Share Drawing"));
    }

    // Method to show settings
    private void showSettings() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Settings");

        // Create a list of settings options
        String[] settingsOptions = {"Canvas Background Color", "Default Brush Size", "Grid Size"};

        builder.setItems(settingsOptions, (dialog, which) -> {
            switch (which) {
                case 0: // Canvas Background Color
                    showColorPickerDialog("Select Background Color", color -> {
                        drawingView.setBackgroundColor(color);
                        drawingView.setBg(color);
                    });
                    break;
                case 1: // Default Brush Size
                    showBrushSizeDialog();
                    break;
                case 2: // Grid Size
                    showGridSizeDialog();
                    break;
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // Helper method to show color picker
    private void showColorPickerDialog(String title, ColorSelectedListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(title);

        // Create a simple color grid
        String[] colorHex = {"#FFFFFF", "#F5F5F5", "#E0E0E0", "#C0C0C0",
                "#000000", "#FF0000", "#00FF00", "#0000FF",
                "#FFFF00", "#FF00FF", "#00FFFF", "#808080"};
        String[] colorOptions = {"White", "White Smoke", "Light Gray", "Silver",
                "Black", "Red", "Green", "Blue",
                "Yellow", "Magenta", "Cyan", "Gray"};

        ListView colorListView = new ListView(MainActivity.this);
        ArrayAdapter<String> colorAdapter = new ArrayAdapter<>(MainActivity.this,
                android.R.layout.simple_list_item_1, colorOptions);
        colorListView.setAdapter(colorAdapter);

        builder.setView(colorListView);
        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();

        colorListView.setOnItemClickListener((parent, view, position, id) -> {
            int color = Color.parseColor(colorHex[position]);
            listener.onColorSelected(color);
            dialog.dismiss();
        });

        dialog.show();
    }

    // Interface for color selection callback
    interface ColorSelectedListener {
        void onColorSelected(int color);
    }
    private void exportAllPagesAsPDF() {
        // Save current page state
        if (currentPageIndex < pages.size()) {
            pages.set(currentPageIndex, drawingView.getDrawingData());
        }

        Map<String, Object> currentState = drawingView.getDrawingData();
        PdfDocument document = new PdfDocument();

        try {
            for (int i = 0; i < pages.size(); i++) {
                drawingView.setDrawingData(pages.get(i));
                Bitmap bitmap = createHighQualityBitmap();

                if (bitmap != null) {
                    // Create page with proper dimensions
                    PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                            bitmap.getWidth(),
                            bitmap.getHeight(),
                            i + 1
                    ).create();

                    PdfDocument.Page page = document.startPage(pageInfo);
                    Canvas canvas = page.getCanvas();

                    // Draw with proper color handling
                    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    paint.setFilterBitmap(true);
                    paint.setDither(true);
                    canvas.drawBitmap(bitmap, 0, 0, paint);

                    document.finishPage(page);
                    bitmap.recycle();
                }
            }

            // Restore original state
            drawingView.setDrawingData(currentState);

            String fileName = "drawing_" + System.currentTimeMillis() + ".pdf";
            File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);

            if (!directory.exists() && !directory.mkdirs()) {
                Toast.makeText(this, "Failed to create directory", Toast.LENGTH_SHORT).show();
                return;
            }

            File file = new File(directory, fileName);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                document.writeTo(fos);
                document.close();
                Toast.makeText(this, "PDF saved to " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                MediaScannerConnection.scanFile(this, new String[]{file.getAbsolutePath()}, null, null);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save PDF", Toast.LENGTH_SHORT).show();
        }
    }
    private Bitmap createHighQualityBitmap() {
        // Get visible drawing dimensions
        int width = drawingView.getWidth();
        int height = drawingView.getHeight();

        if (width <= 0 || height <= 0) return null;

        // Create bitmap with proper configuration
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Draw white background
        canvas.drawColor(drawingView.getBackgroundColor());

        // Draw the view content
        drawingView.draw(canvas);

        return bitmap;
    }

    private void showBrushSizeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Default Brush Size");
        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(50);
        seekBar.setProgress((int)drawingView.getBrushSize());

        builder.setView(seekBar);
        builder.setPositiveButton("Save", (dialog, which) -> {
            int brushSize = seekBar.getProgress();
            drawingView.setBrushSize(brushSize);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // Helper method for grid size dialog
    private void showGridSizeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Grid Size");

        String[] gridSizes = {"Small", "Medium", "Large", "Off"};
        builder.setItems(gridSizes, (dialog, which) -> {
            // Implement grid size changes
            switch (which) {
                case 0: // Small
                    drawingView.setGridSize(30);
                    break;
                case 1: // Medium
                    drawingView.setGridSize(50);
                    break;
                case 2: // Large
                    drawingView.setGridSize(70);
                    break;
                case 3: // Off
                    drawingView.setGridSize(0);
                    break;
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}