/*
 * Copyright 2013-2014 SmartBear Software
 * Copyright 2014-2015 The TestFX Contributors
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence"); You may
 * not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 * http://ec.europa.eu/idabc/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the Licence for the
 * specific language governing permissions and limitations under the Licence.
 */
package org.testfx.service.finder.impl;

import java.util.List;
import java.util.regex.Pattern;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.PopupWindow;
import javafx.stage.Stage;
import javafx.stage.Window;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import org.testfx.service.finder.WindowFinder;

public class WindowFinderImpl implements WindowFinder {

    //---------------------------------------------------------------------------------------------
    // PRIVATE FIELDS.
    //---------------------------------------------------------------------------------------------

    private Window lastTargetWindow;

    //---------------------------------------------------------------------------------------------
    // METHODS.
    //---------------------------------------------------------------------------------------------

    @Override
    public Window targetWindow() {
        return lastTargetWindow;
    }

    @Override
    public void targetWindow(Window window) {
        lastTargetWindow = window;
    }

    @Override
    public void targetWindow(Predicate<Window> predicate) {
        targetWindow(window(predicate));
    }

    @Override
    public List<Window> listWindows() {
        return fetchWindowsInQueue();
    }

    @Override
    public List<Window> listTargetWindows() {
        return fetchWindowsByProximityTo(lastTargetWindow);
    }

    @Override
    public Window window(Predicate<Window> predicate) {
        List<Window> windows = fetchWindowsByProximityTo(lastTargetWindow);
        return Iterables.find(windows, predicate);
    }

    // Convenience methods:

    @Override
    public void targetWindow(int windowIndex) {
        targetWindow(window(windowIndex));
    }

    @Override
    public void targetWindow(String stageTitleRegex) {
        targetWindow(window(stageTitleRegex));
    }

    @Override
    public void targetWindow(Pattern stageTitlePattern) {
        targetWindow(window(stageTitlePattern));
    }

    @Override
    public void targetWindow(Scene scene) {
        targetWindow(window(scene));
    }

    @Override
    public void targetWindow(Node node) {
        targetWindow(window(node));
    }

    @Override
    public Window window(int windowIndex) {
        List<Window> windows = fetchWindowsByProximityTo(lastTargetWindow);
        return windows.get(windowIndex);
    }

    @Override
    public Window window(String stageTitleRegex) {
        return window(hasStageTitlePredicate(stageTitleRegex));
    }

    @Override
    public Window window(Pattern stageTitlePattern) {
        return window(hasStageTitlePredicate(stageTitlePattern.toString()));
    }

    @Override
    public Window window(Scene scene) {
        return scene.getWindow();
    }

    @Override
    public Window window(Node node) {
        return window(node.getScene());
    }

    //---------------------------------------------------------------------------------------------
    // PRIVATE METHODS.
    //---------------------------------------------------------------------------------------------

    @SuppressWarnings("deprecation")
    private List<Window> fetchWindowsInQueue() {
        List<Window> windows = Lists.newArrayList(Window.impl_getWindows());
        return ImmutableList.copyOf(Lists.reverse(windows));
    }

    private List<Window> fetchWindowsByProximityTo(Window targetWindow) {
        List<Window> windows = fetchWindowsInQueue();
        List<Window> windowsByProximity = orderWindowsByProximityTo(targetWindow, windows);
        return windowsByProximity;
    }

    private List<Window> orderWindowsByProximityTo(Window targetWindow,
                                                   List<Window> windows) {
        return Ordering.natural()
            .onResultOf(calculateWindowProximityFunction(targetWindow))
            .immutableSortedCopy(windows);
    }

    private Function<Window, Integer> calculateWindowProximityFunction(Window targetWindow) {
        return window -> calculateWindowProximityTo(targetWindow, window);
    }

    private int calculateWindowProximityTo(Window targetWindow,
                                           Window window) {
        if (window == targetWindow) {
            return 0;
        }
        if (isOwnerOf(window, targetWindow)) {
            return 1;
        }
        return 2;
    }

    private boolean isOwnerOf(Window window,
                              Window targetWindow) {
        Window ownerWindow = retrieveOwnerOf(window);
        if (ownerWindow == targetWindow) {
            return true;
        }
        return ownerWindow != null && isOwnerOf(ownerWindow, targetWindow);
    }

    private Window retrieveOwnerOf(Window window) {
        if (window instanceof Stage) {
            return ((Stage) window).getOwner();
        }
        if (window instanceof PopupWindow) {
            return ((PopupWindow) window).getOwnerWindow();
        }
        return null;
    }

    private Predicate<Window> hasStageTitlePredicate(String stageTitleRegex) {
        return window -> window instanceof Stage &&
            hasStageTitle((Stage) window, stageTitleRegex);
    }

    private boolean hasStageTitle(Stage stage,
                                  String stageTitleRegex) {
        return stage.getTitle() != null && stage.getTitle().matches(stageTitleRegex);
    }

}
