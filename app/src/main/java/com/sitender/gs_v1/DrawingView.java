package com.sitender.gs_v1;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.EditText;
import android.app.AlertDialog;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class DrawingView extends View {
    private Paint paint;
    private Path path;
    private int currentColor = Color.BLACK;
    private float strokeWidth = 10f;
    private boolean isEraserMode = false;
    private int backgroundColor = Color.WHITE;
    private Stack<DrawAction> undoStack = new Stack<>();
    private final Stack<DrawAction> redoStack = new Stack<>();

    private float zoomLevel = 1.0f;
    private float translateX = 0;
    private float translateY = 0;
    private float lastTouchX;
    private float lastTouchY;
    private boolean isPanning = false;

    private boolean showGrid = false;
    private int gridSize = 50;

    private ScaleGestureDetector scaleGestureDetector;
    private float scaleFactor = 1.0f;

    private List<TextElement> textElements = new ArrayList<>();
    private ArrayList<Layer> layers = new ArrayList<>();
    private int currentLayerIndex = 0;

    private GestureDetector gestureDetector;
    private int selectedTextIndex = -1;

    public float getBrushSize() {
        return strokeWidth;
    }

    public enum ToolType {
        PEN, PENCIL, ERASER, TEXT
    }

    private ToolType currentTool = ToolType.PEN;

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeDrawingView();
        layers.add(new Layer("Layer 1", true));
    }

    private void initializeDrawingView() {
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);
        paint.setAntiAlias(true);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        path = new Path();

        gestureDetector = new GestureDetector(getContext(), new GestureListener());
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleListener());

        setLayerType(LAYER_TYPE_HARDWARE, null);
        setFocusable(true);
        setFocusableInTouchMode(true);

        SharedPreferences prefs = getContext().getSharedPreferences("DrawingPrefs", MODE_PRIVATE);
        int defaultSize = 10;
        float initialSize = prefs.getInt("brush_size", defaultSize);
        setBrushSize(initialSize);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(backgroundColor);
        super.onDraw(canvas);
        canvas.save();
        canvas.scale(zoomLevel * scaleFactor, zoomLevel * scaleFactor);
        canvas.translate(translateX / (zoomLevel * scaleFactor), translateY / (zoomLevel * scaleFactor));

        if (showGrid) {
            drawGrid(canvas);
        }

        for (Layer layer : layers) {
            if (layer.isVisible()) {
                drawLayer(canvas, layer);
            }
        }

        if (currentLayerIndex >= 0 && currentLayerIndex < layers.size()) {
            paint.setColor(currentColor);
            paint.setStrokeWidth(strokeWidth);

            if (isEraserMode) {
                paint.setColor(backgroundColor);
            } else {
                paint.setXfermode(null);
            }

            if (currentTool == ToolType.PENCIL) {
                paint.setStrokeCap(Paint.Cap.SQUARE);
                paint.setStrokeJoin(Paint.Join.BEVEL);
                paint.setAlpha(200);
            } else {
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStrokeJoin(Paint.Join.ROUND);
                paint.setAlpha(255);
            }

            canvas.drawPath(path, paint);
        }

        for (TextElement textElement : textElements) {
            Paint textPaint = new Paint();
            textPaint.setColor(textElement.color);
            textPaint.setTextSize(textElement.size);
            textPaint.setAntiAlias(true);
            canvas.drawText(textElement.text, textElement.x, textElement.y, textPaint);
        }

        canvas.restore();
    }

    private void drawLayer(Canvas canvas, Layer layer) {
        for (int i = 0; i < layer.paths.size(); i++) {
            paint.setColor(layer.colors.get(i));
            paint.setStrokeWidth(layer.strokes.get(i));

            if (layer.toolTypes.get(i) == ToolType.PENCIL) {
                paint.setStrokeCap(Paint.Cap.SQUARE);
                paint.setStrokeJoin(Paint.Join.BEVEL);
                paint.setAlpha(200);
            } else {
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStrokeJoin(Paint.Join.ROUND);
                paint.setAlpha(255);
            }

            if (layer.isEraser.get(i)) {
                paint.setColor(backgroundColor);
            } else {
                paint.setXfermode(null);
            }

            canvas.drawPath(layer.paths.get(i), paint);
        }

        paint.setXfermode(null);
    }

    private void drawGrid(Canvas canvas) {
        Paint gridPaint = new Paint();
        gridPaint.setColor(Color.LTGRAY);
        gridPaint.setStrokeWidth(1);

        int width = getWidth();
        int height = getHeight();

        for (int i = 0; i < width; i += gridSize) {
            canvas.drawLine(i, 0, i, height, gridPaint);
        }

        for (int i = 0; i < height; i += gridSize) {
            canvas.drawLine(0, i, width, i, gridPaint);
        }
    }
    private float getMidpointX(MotionEvent event) {
        return (event.getX(0) + event.getX(1)) / 2;
    }

    private float getMidpointY(MotionEvent event) {
        return (event.getY(0) + event.getY(1)) / 2;
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = (event.getX() - translateX) / (zoomLevel * scaleFactor);
        float y = (event.getY() - translateY) / (zoomLevel * scaleFactor);

        // Handle text tool interactions
        if (currentTool == ToolType.TEXT) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    for (int i = textElements.size() - 1; i >= 0; i--) {
                        TextElement textElement = textElements.get(i);
                        if (getTextBounds(textElement).contains(x, y)) {
                            selectedTextIndex = i;
                            lastTouchX = x;
                            lastTouchY = y;
                            return true;
                        }
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (selectedTextIndex != -1) {
                        TextElement text = textElements.get(selectedTextIndex);
                        float dx = x - lastTouchX;
                        float dy = y - lastTouchY;
                        text.x += dx;
                        text.y += dy;
                        lastTouchX = x;
                        lastTouchY = y;
                        invalidate();
                        return true;
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (selectedTextIndex != -1) {
                        DrawAction action = new DrawAction(currentLayerIndex, -4);
                        action.textIndex = selectedTextIndex;
                        action.savedTextElement = new TextElement(
                                textElements.get(selectedTextIndex).text,
                                textElements.get(selectedTextIndex).x,
                                textElements.get(selectedTextIndex).y,
                                textElements.get(selectedTextIndex).color,
                                textElements.get(selectedTextIndex).size
                        );
                        undoStack.push(action);
                        selectedTextIndex = -1;
                    } else if (event.getPointerCount() == 1) {
                        showTextDialog(x, y);
                    }
                    return true;
            }
        }

        // Handle drawing with pen, pencil, or eraser (single finger)
        if ((currentTool == ToolType.PEN || currentTool == ToolType.PENCIL || currentTool == ToolType.ERASER)
                && event.getPointerCount() == 1 && !isPanning) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    redoStack.clear();
                    path = new Path();
                    path.moveTo(x, y);
                    invalidate();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    path.lineTo(x, y);
                    invalidate();
                    return true;

                case MotionEvent.ACTION_UP:
                    if (currentLayerIndex >= 0 && currentLayerIndex < layers.size()) {
                        Layer currentLayer = layers.get(currentLayerIndex);
                        currentLayer.paths.add(path);
                        currentLayer.colors.add(currentColor);
                        currentLayer.strokes.add(strokeWidth);
                        currentLayer.isEraser.add(isEraserMode);
                        currentLayer.toolTypes.add(currentTool);
                        undoStack.push(new DrawAction(currentLayerIndex, currentLayer.paths.size() - 1));
                    }
                    path = new Path();
                    invalidate();
                    return true;
            }
        }

        // Handle two-finger panning
        if (event.getPointerCount() == 2) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_POINTER_DOWN:
                    isPanning = true;
                    lastTouchX = getMidpointX(event);
                    lastTouchY = getMidpointY(event);
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (isPanning) {
                        float currentMidX = getMidpointX(event);
                        float currentMidY = getMidpointY(event);
                        float dx = currentMidX - lastTouchX;
                        float dy = currentMidY - lastTouchY;
                        translateX += dx;
                        translateY += dy;
                        lastTouchX = currentMidX;
                        lastTouchY = currentMidY;
                        invalidate();
                    }
                    break;

                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_UP:
                    isPanning = false;
                    break;
            }
        }

        // Pass event to scale and gesture detectors
        boolean scaleHandled = scaleGestureDetector.onTouchEvent(event);
        boolean gestureHandled = gestureDetector.onTouchEvent(event);
        if (scaleHandled || gestureHandled) return true;

        return super.onTouchEvent(event);
    }
    public void setDrawingData(Map<String, Object> data) {
        if (data == null) return;

        // Set background color
        if (data.containsKey("backgroundColor")) {
            backgroundColor = (int) data.get("backgroundColor");
        }

        // Clear current layers and text elements
        layers.clear();
        textElements.clear();

        // Load layers
        if (data.containsKey("layers")) {
            List<Map<String, Object>> layersData = (List<Map<String, Object>>) data.get("layers");
            for (Map<String, Object> layerData : layersData) {
                String name = (String) layerData.get("name");
                boolean visible = (boolean) layerData.get("visible");
                Layer layer = new Layer(name, visible);
                List<Map<String, Object>> pathDataList = (List<Map<String, Object>>) layerData.get("pathData");
                for (Map<String, Object> pathData : pathDataList) {
                    float[] coordinates = (float[]) pathData.get("pathCoordinates");
                    Path path = new Path();
                    if (coordinates.length >= 2) {
                        path.moveTo(coordinates[0], coordinates[1]);
                        for (int i = 2; i < coordinates.length; i += 2) {
                            path.lineTo(coordinates[i], coordinates[i + 1]);
                        }
                    }
                    layer.paths.add(path);
                    layer.colors.add((int) pathData.get("color"));
                    layer.strokes.add((float) pathData.get("strokeWidth"));
                    layer.isEraser.add((boolean) pathData.get("isEraser"));
                    String toolTypeStr = (String) pathData.get("toolType");
                    layer.toolTypes.add(ToolType.valueOf(toolTypeStr));
                }
                layers.add(layer);
            }
        }

        // Load text elements
        if (data.containsKey("textElements")) {
            List<Map<String, Object>> textsData = (List<Map<String, Object>>) data.get("textElements");
            for (Map<String, Object> textData : textsData) {
                String text = (String) textData.get("text");
                float x = (float) textData.get("x");
                float y = (float) textData.get("y");
                int color = (int) textData.get("color");
                float size = (float) textData.get("size");
                textElements.add(new TextElement(text, x, y, color, size));
            }
        }

        // Set current layer index
        if (data.containsKey("currentLayerIndex")) {
            currentLayerIndex = (int) data.get("currentLayerIndex");
        }

        // Set grid settings
        if (data.containsKey("showGrid")) {
            showGrid = (boolean) data.get("showGrid");
        }
        if (data.containsKey("gridSize")) {
            gridSize = (int) data.get("gridSize");
        }

        // Set zoom and translation
        if (data.containsKey("zoomLevel")) {
            zoomLevel = (float) data.get("zoomLevel");
        }
        if (data.containsKey("translateX")) {
            translateX = (float) data.get("translateX");
        }
        if (data.containsKey("translateY")) {
            translateY = (float) data.get("translateY");
        }

        // Reset scale factor (transient pinch-zoom factor)
        scaleFactor = 1.0f;

        // Reset the current path to ensure no residual drawing
        path = new Path();

        // Redraw the view
        invalidate();
    }
    private void showTextDialog(float x, float y) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Add Text");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 48, 48, 48);

        EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Enter text");

        SeekBar sizeSeekBar = new SeekBar(getContext());
        sizeSeekBar.setMax(90);
        sizeSeekBar.setProgress(40);
        TextView sizeLabel = new TextView(getContext());
        sizeLabel.setText("Size: 40px");

        sizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sizeLabel.setText("Size: " + (progress + 10) + "px");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        layout.addView(input);
        layout.addView(sizeLabel);
        layout.addView(sizeSeekBar);
        builder.setView(layout);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String text = input.getText().toString();
            if (!text.isEmpty()) {
                int textSize = sizeSeekBar.getProgress() + 10;
                textElements.add(new TextElement(text, x, y, currentColor, textSize));
                invalidate();
                DrawAction action = new DrawAction(currentLayerIndex, -2);
                action.textIndex = textElements.size() - 1;
                undoStack.push(action);
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    public void setBrushColor(int color) {
        currentColor = color;
        isEraserMode = false;
    }

    public void setBrushSize(float size) {
        strokeWidth = size;
    }
    public int getBackgroundColor(){
        return this.backgroundColor;
    }

    public void enableEraser() {
        isEraserMode = true;
        currentTool = ToolType.ERASER;
        strokeWidth = 20f;
        paint.setColor(backgroundColor);
    }

    public void setTool(ToolType tool) {
        currentTool = tool;
        isEraserMode = (tool == ToolType.ERASER);
        if (tool != ToolType.ERASER && isEraserMode) {
            strokeWidth = 10f;
        }
    }

    public void clearCanvas() {
        if (currentLayerIndex >= 0 && currentLayerIndex < layers.size()) {
            Layer currentLayer = layers.get(currentLayerIndex);
            DrawAction action = new DrawAction(currentLayerIndex, -1);
            action.savedPaths = new ArrayList<>(currentLayer.paths);
            action.savedColors = new ArrayList<>(currentLayer.colors);
            action.savedStrokes = new ArrayList<>(currentLayer.strokes);
            action.savedIsEraser = new ArrayList<>(currentLayer.isEraser);
            action.savedToolTypes = new ArrayList<>(currentLayer.toolTypes);
            undoStack.push(action);

            currentLayer.paths.clear();
            currentLayer.colors.clear();
            currentLayer.strokes.clear();
            currentLayer.isEraser.clear();
            currentLayer.toolTypes.clear();

            if (!textElements.isEmpty()) {
                DrawAction textAction = new DrawAction(currentLayerIndex, -3);
                textAction.savedTextElements = new ArrayList<>(textElements);
                undoStack.push(textAction);
                textElements.clear();
            }

            invalidate();
        }
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            DrawAction action = undoStack.pop();
            redoStack.push(action);

            if (action.pathIndex == -1) {
                if (action.savedPaths != null) {
                    Layer layer = layers.get(action.layerIndex);
                    layer.paths = action.savedPaths;
                    layer.colors = action.savedColors;
                    layer.strokes = action.savedStrokes;
                    layer.isEraser = action.savedIsEraser;
                    layer.toolTypes = action.savedToolTypes;
                }
            } else if (action.pathIndex == -2) {
                if (action.textIndex >= 0 && action.textIndex < textElements.size()) {
                    action.savedTextElement = textElements.get(action.textIndex);
                    textElements.remove(action.textIndex);
                }
            } else if (action.pathIndex == -3) {
                textElements = action.savedTextElements;
            } else if (action.pathIndex == -4) {
                if (action.textIndex >= 0 && action.textIndex < textElements.size()) {
                    TextElement current = textElements.get(action.textIndex);
                    float tempX = current.x;
                    float tempY = current.y;
                    current.x = action.savedTextElement.x;
                    current.y = action.savedTextElement.y;
                    action.savedTextElement.x = tempX;
                    action.savedTextElement.y = tempY;
                    redoStack.push(action);
                    invalidate();
                }
            } else if (action.pathIndex == -5) {
                if (action.textIndex >= 0 && action.savedTextElement != null) {
                    textElements.add(action.textIndex, action.savedTextElement);
                    invalidate();
                }
            } else {
                Layer layer = layers.get(action.layerIndex);
                if (layer.paths.size() > 0) {
                    action.savedPath = layer.paths.get(action.pathIndex);
                    action.savedColor = layer.colors.get(action.pathIndex);
                    action.savedStroke = layer.strokes.get(action.pathIndex);
                    action.savedIsEraserFlag = layer.isEraser.get(action.pathIndex);
                    action.savedToolType = layer.toolTypes.get(action.pathIndex);

                    layer.paths.remove(action.pathIndex);
                    layer.colors.remove(action.pathIndex);
                    layer.strokes.remove(action.pathIndex);
                    layer.isEraser.remove(action.pathIndex);
                    layer.toolTypes.remove(action.pathIndex);
                }
            }

            invalidate();
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            DrawAction action = redoStack.pop();
            undoStack.push(action);

            if (action.pathIndex == -1) {
                if (action.layerIndex >= 0 && action.layerIndex < layers.size()) {
                    Layer layer = layers.get(action.layerIndex);
                    layer.paths.clear();
                    layer.colors.clear();
                    layer.strokes.clear();
                    layer.isEraser.clear();
                    layer.toolTypes.clear();
                }
            } else if (action.pathIndex == -2) {
                if (action.savedTextElement != null) {
                    textElements.add(action.savedTextElement);
                }
            } else if (action.pathIndex == -3) {
                textElements.clear();
            } else if (action.pathIndex == -4) {
                if (action.textIndex >= 0 && action.textIndex < textElements.size()) {
                    TextElement current = textElements.get(action.textIndex);
                    float tempX = current.x;
                    float tempY = current.y;
                    current.x = action.savedTextElement.x;
                    current.y = action.savedTextElement.y;
                    action.savedTextElement.x = tempX;
                    action.savedTextElement.y = tempY;
                    undoStack.push(action);
                    invalidate();
                }
            } else if (action.pathIndex == -5) {
                if (action.textIndex >= 0 && action.textIndex < textElements.size()) {
                    textElements.remove(action.textIndex);
                    invalidate();
                }
            } else {
                if (action.layerIndex >= 0 && action.layerIndex < layers.size() && action.savedPath != null) {
                    Layer layer = layers.get(action.layerIndex);
                    layer.paths.add(action.savedPath);
                    layer.colors.add(action.savedColor);
                    layer.strokes.add(action.savedStroke);
                    layer.isEraser.add(action.savedIsEraserFlag);
                    layer.toolTypes.add(action.savedToolType);
                }
            }

            invalidate();
        }
    }

    public void setZoomLevel(float level) {
        zoomLevel = Math.max(0.5f, Math.min(level, 3.0f));
        invalidate();
    }

    public void resetTranslation() {
        translateX = 0;
        translateY = 0;
        scaleFactor = 1.0f;
        invalidate();
    }

    public void toggleGrid() {
        showGrid = !showGrid;
        invalidate();
    }

    public void addLayer(String name) {
        layers.add(new Layer(name, true));
        currentLayerIndex = layers.size() - 1;
        invalidate();
    }

    public void removeCurrentLayer() {
        if (layers.size() > 1 && currentLayerIndex >= 0) {
            layers.remove(currentLayerIndex);
            currentLayerIndex = Math.max(0, currentLayerIndex - 1);
            invalidate();
        }
    }

    public void setCurrentLayer(int index) {
        if (index >= 0 && index < layers.size()) {
            currentLayerIndex = index;
            invalidate();
        }
    }

    public List<String> getLayerNames() {
        List<String> names = new ArrayList<>();
        for (Layer layer : layers) {
            names.add(layer.name);
        }
        return names;
    }

    public void setGridSize(int size) {
        this.gridSize = Math.max(10, Math.min(size, 200));
        if (showGrid) {
            invalidate();
        }
    }

    public Map<String, Object> getDrawingData() {
        Map<String, Object> drawingData = new HashMap<>();
        drawingData.put("backgroundColor", backgroundColor);
        List<Map<String, Object>> layersData = new ArrayList<>();
        for (Layer layer : layers) {
            Map<String, Object> layerData = new HashMap<>();
            layerData.put("name", layer.name);
            layerData.put("visible", layer.visible);
            layerData.put("pathData", serializePathsData(layer));
            layersData.add(layerData);
        }
        drawingData.put("layers", layersData);

        List<Map<String, Object>> textsData = new ArrayList<>();
        for (TextElement textElement : textElements) {
            Map<String, Object> textData = new HashMap<>();
            textData.put("text", textElement.text);
            textData.put("x", textElement.x);
            textData.put("y", textElement.y);
            textData.put("color", textElement.color);
            textData.put("size", textElement.size);
            textsData.add(textData);
        }
        drawingData.put("textElements", textsData);

        drawingData.put("currentLayerIndex", currentLayerIndex);
        drawingData.put("showGrid", showGrid);
        drawingData.put("gridSize", gridSize);
        drawingData.put("zoomLevel", zoomLevel);
        drawingData.put("translateX", translateX);
        drawingData.put("translateY", translateY);

        return drawingData;
    }

    private List<Map<String, Object>> serializePathsData(Layer layer) {
        List<Map<String, Object>> pathsData = new ArrayList<>();
        for (int i = 0; i < layer.paths.size(); i++) {
            Map<String, Object> pathData = new HashMap<>();
            Path path = layer.paths.get(i);

            List<Float> pointsList = new ArrayList<>();
            PathMeasure measure = new PathMeasure(path, false);
            float[] pos = new float[2];
            float distance = 0f;
            float pathLength = measure.getLength();
            float interval = 1f;
            while (distance <= pathLength) {
                if (measure.getPosTan(distance, pos, null)) {
                    pointsList.add(pos[0]);
                    pointsList.add(pos[1]);
                }
                distance += interval;
            }

            float[] finalPoints = new float[pointsList.size()];
            for (int j = 0; j < finalPoints.length; j++) {
                finalPoints[j] = pointsList.get(j);
            }

            pathData.put("pathCoordinates", finalPoints);
            pathData.put("color", layer.colors.get(i));
            pathData.put("strokeWidth", layer.strokes.get(i));
            pathData.put("isEraser", layer.isEraser.get(i));
            pathData.put("toolType", layer.toolTypes.get(i).name());
            pathsData.add(pathData);
        }
        return pathsData;
    }

    private class TextElement {
        String text;
        float x, y;
        int color;
        float size;

        TextElement(String text, float x, float y, int color, float size) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.color = color;
            this.size = size;
        }
    }

    private class Layer {
        String name;
        boolean visible;
        ArrayList<Path> paths = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();
        ArrayList<Float> strokes = new ArrayList<>();
        ArrayList<Boolean> isEraser = new ArrayList<>();
        ArrayList<ToolType> toolTypes = new ArrayList<>();

        Layer(String name, boolean visible) {
            this.name = name;
            this.visible = visible;
        }

        boolean isVisible() {
            return visible;
        }

        void setVisible(boolean visible) {
            this.visible = visible;
            invalidate();
        }
    }

    private class DrawAction {
        int layerIndex;
        int pathIndex;
        int textIndex = -1;
        ArrayList<Path> savedPaths;
        ArrayList<Integer> savedColors;
        ArrayList<Float> savedStrokes;
        ArrayList<Boolean> savedIsEraser;
        ArrayList<ToolType> savedToolTypes;
        ArrayList<TextElement> savedTextElements;
        Path savedPath;
        Integer savedColor;
        Float savedStroke;
        Boolean savedIsEraserFlag;
        ToolType savedToolType;
        TextElement savedTextElement;

        DrawAction(int layerIndex, int pathIndex) {
            this.layerIndex = layerIndex;
            this.pathIndex = pathIndex;

            if (pathIndex >= 0 && layerIndex >= 0 && layerIndex < layers.size()) {
                Layer layer = layers.get(layerIndex);
                if (pathIndex < layer.paths.size()) {
                    this.savedPath = new Path(layer.paths.get(pathIndex));
                    this.savedColor = layer.colors.get(pathIndex);
                    this.savedStroke = layer.strokes.get(pathIndex);
                    this.savedIsEraserFlag = layer.isEraser.get(pathIndex);
                    this.savedToolType = layer.toolTypes.get(pathIndex);
                }
            }
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(0.5f, Math.min(scaleFactor, 3.0f));
            invalidate();
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (currentTool == ToolType.TEXT) {
                float x = (e.getX() - translateX) / (zoomLevel * scaleFactor);
                float y = (e.getY() - translateY) / (zoomLevel * scaleFactor);
                for (int i = textElements.size() - 1; i >= 0; i--) {
                    TextElement textElement = textElements.get(i);
                    if (getTextBounds(textElement).contains(x, y)) {
                        DrawAction action = new DrawAction(currentLayerIndex, -5);
                        action.textIndex = i;
                        action.savedTextElement = new TextElement(
                                textElement.text, textElement.x, textElement.y,
                                textElement.color, textElement.size
                        );
                        undoStack.push(action);
                        textElements.remove(i);
                        invalidate();
                        return true;
                    }
                }
            }
            if (currentTool == ToolType.ERASER) {
                clearCanvas();
                return true;
            }
            return false;
        }
    }

    private RectF getTextBounds(TextElement textElement) {
        Paint tempPaint = new Paint();
        tempPaint.setTextSize(textElement.size);
        float width = tempPaint.measureText(textElement.text);
        Paint.FontMetrics fm = tempPaint.getFontMetrics();
        return new RectF(
                textElement.x,
                textElement.y + fm.ascent,
                textElement.x + width,
                textElement.y + fm.descent
        );
    }

    public void setBg(int color) {
        this.backgroundColor = color;
    }
}