/*
 * Licensed to the Fitting Project under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Fitting Project licenses
 * this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.fitting.selenium;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.fitting.Dimension;
import org.fitting.*;
import org.fitting.NoSuchElementException;
import org.openqa.selenium.*;
import org.openqa.selenium.SearchContext;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.fitting.selenium.SeleniumDataTypeConverter.convert;

/** {@link org.fitting.ElementContainer} for Selenium based browser windows. */
public class SeleniumWindow implements ElementContainer, SeleniumSearchContext {
    /** The id of an (highly likely to be) unknown element. */
    private static final String UNKNOWN_ELEMENT_ID = "unknown_element_id_x3asdf4hd462345";
    /** The id of the window. */
    private final String id;
    /** The parent id of the window. */
    private final String parentId;
    /** The selector used to select the current frame. */
    private org.openqa.selenium.By currentFrameSelector;
    /** The underlying web driver. */
    private final WebDriver driver;

    /**
     * Create a new SeleniumWindow.
     *
     * @param id     The window id.
     * @param driver The underlying web driver for the window.
     */
    public SeleniumWindow(String id, WebDriver driver) {
        this(id, null, driver);
    }

    /**
     * Create a new SeleniumWindow.
     *
     * @param id       The window id.
     * @param parentId The ID of the parent window.
     * @param driver   The underlying web driver for the window.
     */
    public SeleniumWindow(String id, String parentId, WebDriver driver) {
        this.driver = driver;
        this.id = id;
        this.parentId = parentId;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getParentId() {
        return parentId;
    }

    @Override
    public boolean hasParent() {
        return !isEmpty(parentId);
    }

    @Override
    public boolean isRootContainer() {
        return true;
    }

    @Override
    public void refresh() {
        driver.navigate().refresh();
    }

    /**
     * Select the frame with id.
     *
     * @param id     The frame id.
     * @param driver The WebDriver.
     */
    public void selectFrameWithId(final String id, final WebDriver driver) {
        final org.openqa.selenium.By frameSelector = org.openqa.selenium.By.id(id);
        final WebElement element = driver.findElement(frameSelector);
        driver.switchTo().frame(element);
        this.currentFrameSelector = frameSelector;
    }

    /**
     * Select the frame with name.
     *
     * @param name   The frame name.
     * @param driver The WebDriver.
     */
    public void selectFrameWithName(final String name, final WebDriver driver) {
        final org.openqa.selenium.By frameSelector = org.openqa.selenium.By.name(name);
        final WebElement element = driver.findElement(frameSelector);
        driver.switchTo().frame(element);
        this.currentFrameSelector = frameSelector;
    }

    /**
     * Select the main frame.
     *
     * @param driver The WebDriver.
     */
    public void selectMainFrame(final WebDriver driver) {
        driver.switchTo().defaultContent();
        currentFrameSelector = org.openqa.selenium.By.name("_top");
    }

    /**
     * Get the selected frame id.
     *
     * @return The Selenium By clause with which the frame was selected.
     */
    public org.openqa.selenium.By getSelectedFrameSelectClause() {
        return this.currentFrameSelector;
    }

    /**
     * Create a new window.
     *
     * @param location  The location.
     * @param webDriver The WebDriver.
     *
     * @return window The newly created window.
     */
    public static SeleniumWindow createNewWindow(final String location, final WebDriver webDriver) {
        // TODO Revisit this code and probably rewrite. Let window creation/destruction be handled by the ElementContainerProvider.
        if (!JavascriptExecutor.class.isAssignableFrom(webDriver.getClass())) {
            throw new FittingException("The provided Selenium web driver does not support javascript execution, a new window can not be created.");
        }
        final Set<String> currentHandles = webDriver.getWindowHandles();
        final String currentWindowHandle = webDriver.getWindowHandle();
        final JavascriptExecutor executor = (JavascriptExecutor) webDriver;
        executor.executeScript("window.open('" + location + "')");

        final List<String> newHandles = new ArrayList<String>(webDriver.getWindowHandles());
        newHandles.removeAll(currentHandles);
        if (newHandles.size() != 1) {
            throw new FittingException("There were " + newHandles.size() + " windows created, while 1 was expected.");
        }
        final String newWindowHandle = newHandles.get(0);
        return new SeleniumWindow(newWindowHandle, currentWindowHandle, webDriver);
    }

    /**
     * Switch to another window.
     *
     * @param handle    The id of the window.
     * @param webDriver The Selenium web driver to use.
     *
     * @return The window.
     */
    public static SeleniumWindow switchToWindow(final String handle, final WebDriver webDriver) {
        // TODO Revisit this code and probably rewrite. Let window creation/destruction be handled by the ElementContainerProvider.
        final String currentWindowHandle = webDriver.getWindowHandle();
        try {
            webDriver.switchTo().window(handle);
        } catch (NoSuchWindowException e) {
            throw new FittingException(String.format("There is no window with the current id[%s] present.", handle));
        }
        return new SeleniumWindow(webDriver.getWindowHandle(), currentWindowHandle, webDriver);
    }

    @Override
    public Dimension getSize() {
        return convert(driver.manage().window().getSize());
    }

    @Override
    public void setSize(final Dimension size) throws FittingException {
        driver.manage().window().setSize(convert(size));
    }

    @Override
    public boolean isActive() {
        return id.equals(driver.getWindowHandle());
    }

    @Override
    public void activate() {
        if (!isActive()) {
            driver.switchTo().window(id);
        }
    }

    @Override
    public void close() {
        driver.close();
    }

    @Override
    public void navigateTo(String uri) {
        driver.get(uri);
    }

    @Override
    public String currentLocation() {
        return driver.getCurrentUrl();
    }

    @Override
    public String getTitle() {
        return driver.getTitle();
    }

    @Override
    public void waitSeconds(final int seconds) {
        try {
            waitForElement(SeleniumSelector.byId(UNKNOWN_ELEMENT_ID), seconds);
        } catch (NoSuchElementException e) {
            // Ignore.
        }
    }

    @Override
    public boolean isTextPresent(final String text) {
        return false;
    }

    @Override
    public List<Element> findElementsBy(final Selector selector) {
        return convert(driver.findElements(convert(selector)));
    }

    @Override
    public Element findElementBy(final Selector selector) {
        return convert(driver.findElement(convert(selector)));
    }

    @Override
    public void waitForElement(final Selector selector, final int timeout) throws NoSuchElementException {
        SeleniumUtil.waitForElement(driver, this, selector, timeout);
    }

    @Override
    public void waitForElementWithContent(final Selector selector, final String content, final int timeout) {
        SeleniumUtil.waitForElementWithContent(driver, this, selector, content, timeout);
    }

    @Override
    public SearchContext getImplementation() {
        return driver;
    }
}
