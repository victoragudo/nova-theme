from palette import accents, alpha, mix


def shadow(p, ratio):
    return alpha(p["shadow"], min(1.0, ratio * p["shadowStrength"]))


def flame_cell(p, accent):
    base = p[accent]
    return {
        "foreground": p["fg"],
        "hoverForeground": p["fg"],
        "inactiveForeground": p["fgMuted"],
        "hoverInactiveForeground": p["fg"],
        "background": mix(p["island"], base, 0.24),
        "hoverBackground": mix(p["island"], base, 0.34),
        "inactiveBackground": mix(p["island"], base, 0.12),
        "hoverInactiveBackground": mix(p["island"], base, 0.2),
        "searchOkBackground": mix(p["island"], p["green"], 0.3),
        "searchOkHoverBackground": mix(p["island"], p["green"], 0.42),
        "searchOkInactiveBackground": mix(p["island"], p["green"], 0.16),
        "searchOkHoverInactiveBackground": mix(p["island"], p["green"], 0.24),
        "searchFailBackground": mix(p["island"], p["red"], 0.24),
        "searchFailHoverBackground": mix(p["island"], p["red"], 0.34),
        "searchFailInactiveBackground": mix(p["island"], p["red"], 0.12),
        "searchFailHoverInactiveBackground": mix(p["island"], p["red"], 0.2),
    }


def shadow_ring(p):
    return {
        "top0Color": shadow(p, 0.22),
        "top1Color": shadow(p, 0.08),
        "bottom0Color": shadow(p, 0.28),
        "bottom1Color": shadow(p, 0.12),
        "left0Color": shadow(p, 0.22),
        "left1Color": shadow(p, 0.08),
        "right0Color": shadow(p, 0.22),
        "right1Color": shadow(p, 0.08),
        "topLeft0Color": shadow(p, 0.18),
        "topLeft1Color": shadow(p, 0.06),
        "topRight0Color": shadow(p, 0.18),
        "topRight1Color": shadow(p, 0.06),
        "bottomLeft0Color": shadow(p, 0.24),
        "bottomLeft1Color": shadow(p, 0.1),
        "bottomRight0Color": shadow(p, 0.24),
        "bottomRight1Color": shadow(p, 0.1),
    }


def project_gradients(p):
    palette = accents(p)
    groups = {}
    for index, accent in enumerate(palette):
        companion = palette[(index + 1) % len(palette)]
        groups[f"Group{index + 1}"] = {
            "DiagonalGradient": {
                "Color1": mix(p["island"], accent, 0.2),
                "Color2": mix(p["island"], companion, 0.17),
                "Color3": mix(p["island"], accent, 0.2),
                "Color4": mix(p["island"], accent, 0.07),
                "Fraction1": 0.0,
                "Fraction2": 0.13,
                "Fraction3": 0.3,
                "Fraction4": 1.0,
            },
            "RadialGradient": {
                "Color1": alpha(mix(p["island"], accent, 0.5), 1.0),
                "Color2": alpha(mix(p["island"], accent, 0.5), 0.0),
            },
            "HorizontalGradient": {
                "Color1": alpha(p["canvas"], 0.2),
                "Color2": alpha(p["canvas"], 0.3),
            },
            "VerticalGradient": {
                "Color1": alpha(p["canvas"], 0.1),
                "Color2": alpha(p["canvas"], 0.5),
            },
        }
    return groups


def recent_projects(p):
    palette = accents(p)
    entry = {"MainToolbarGradient.width": 600, "MainToolbarGradient.height": 300}
    for index, accent in enumerate(palette):
        companion = palette[(index + 1) % len(palette)]
        entry[f"Color{index + 1}"] = {
            "MainToolbarGradientStart": mix(p["canvas"], accent, 0.35),
            "Avatar": {"Start": accent, "End": companion},
        }
    return entry


def build(p, islands):
    ui = {}
    if islands:
        ui.update(
            {
                "Islands": 1,
                "Island.arc": 20,
                "Island.arc.compact": 16,
                "Island.borderArcLength": 14,
                "Island.borderArcLength.compact": 10,
                "Island.borderWidth": 5,
                "Island.borderWidth.compact": 4,
                "Island.borderColor": p["island"],
                "Island.inactiveAlpha": 0.56,
                "Island.toolWindowAlpha": 0.2,
            }
        )
    ui.update(
        {
            "MainWindow.background": p["canvas"],
            "StatusBar.borderColor": alpha(p["canvas"], 0.0),
            "ToolWindow.Stripe.borderColor": alpha(p["canvas"], 0.0),
            "MainToolbar.borderColor": alpha(p["canvas"], 0.0),
        }
    )
    if islands:
        ui["alt"] = {
            "MainWindow.background": p["canvas"],
            "MainToolbar.background": p["canvas"],
            "StatusBar.background": p["canvas"],
            "ToolWindow.background": p["island"],
            "ToolWindow.Header.inactiveBackground": p["island"],
            "ToolWindow.Stripe.background": p["canvas"],
            "Island.borderColor": p["island"],
            "Borders.color": p["border"],
            "Borders.ContrastBorderColor": p["border"],
        }
    ui["*"] = {
        "foreground": p["fg"],
        "infoForeground": p["fgMuted"],
        "selectionBackground": p["selection"],
        "selectionForeground": p["fg"],
        "selectionInactiveBackground": p["hover"],
        "disabledForeground": p["fgDisabled"],
        "errorForeground": p["red"],
    }
    ui["ActionButton"] = {
        "hoverBackground": p["elevated"],
        "hoverBorderColor": p["elevated"],
        "pressedBackground": p["selection"],
        "pressedBorderColor": p["selection"],
    }
    ui["AgentPicker"] = {"Advertiser.background": p["popup"]}
    ui["Badge"] = {
        "blueBackground": p["cyan"],
        "blueForeground": p["counterFg"],
        "blueSecondaryBackground": mix(p["island"], p["cyan"], 0.22),
        "blueSecondaryForeground": p["cyan"],
        "greenBackground": p["green"],
        "greenForeground": p["counterFg"],
        "greenSecondaryBackground": mix(p["island"], p["green"], 0.22),
        "greenSecondaryForeground": p["green"],
        "greenOutlineForeground": p["green"],
        "greenOutlineBorderColor": mix(p["island"], p["green"], 0.45),
        "purpleSecondaryBackground": mix(p["island"], p["purple"], 0.22),
        "purpleSecondaryForeground": p["purple"],
        "graySecondaryBackground": p["elevated"],
        "graySecondaryForeground": p["fgMuted"],
        "disabledBackground": p["elevated"],
        "disabledForeground": p["fgDisabled"],
    }
    ui["Banner"] = {
        "errorBackground": p["errorSoftBg"],
        "errorBorderColor": p["red"],
        "warningBackground": p["warningSoftBg"],
        "warningBorderColor": p["warning"],
        "successBackground": mix(p["island"], p["green"], 0.16),
        "successBorderColor": p["green"],
        "infoBackground": mix(p["island"], p["cyan"], 0.16),
        "infoBorderColor": p["cyan"],
        "aiBackground": mix(p["island"], p["purple"], 0.16),
        "aiBorderColor": p["purple"],
    }
    ui["Bookmark"] = {
        "iconBackground": p["gold"],
        "Mnemonic.iconForeground": p["fg"],
        "Mnemonic.iconBackground": mix(p["island"], p["gold"], 0.28),
        "Mnemonic.iconBorderColor": p["gold"],
        "MnemonicAssigned.background": mix(p["island"], p["gold"], 0.28),
        "MnemonicAssigned.foreground": p["gold"],
        "MnemonicAvailable.borderColor": p["border"],
        "MnemonicCurrent.background": p["selection"],
    }
    ui["Button"] = {
        "startBackground": p["surface"],
        "endBackground": p["surface"],
        "startBorderColor": p["border"],
        "endBorderColor": p["border"],
        "foreground": p["fg"],
        "shadowColor": shadow(p, 0.12),
        "default": {
            "startBackground": p["magenta"],
            "endBackground": p["purple"],
            "startBorderColor": p["magenta"],
            "endBorderColor": p["purple"],
            "foreground": "#FFFFFF",
            "focusedBorderColor": p["cyan"],
            "shadowColor": alpha(p["magenta"], 0.25),
        },
    }
    ui["Code"] = {
        "Block.backgroundOpacity": 100,
        "Block.borderColor": p["border"],
        "Block.EditorPane.backgroundColor": p["surface"],
        "Block.EditorPane.backgroundOpacity": 100,
        "Block.EditorPane.borderColor": p["border"],
        "Inline.backgroundColor": p["elevated"],
        "Inline.backgroundOpacity": 100,
    }
    ui["ComboBox"] = {
        "background": p["surface"],
        "nonEditableBackground": p["surface"],
        "selectionBackground": p["selection"],
        "ArrowButton": {
            "background": p["surface"],
            "nonEditableBackground": p["surface"],
            "iconColor": p["fg"],
            "disabledIconColor": p["fgDisabled"],
        },
    }
    ui["CompletionPopup"] = {
        "background": p["popup"],
        "foreground": p["fg"],
        "selectionBackground": p["selection"],
        "selectionInactiveBackground": p["hover"],
        "matchForeground": p["gold"],
    }
    ui["Component"] = {
        "background": p["surface"],
        "borderColor": p["border"],
        "focusedBorderColor": p["magenta"],
        "errorFocusColor": p["red"],
        "inactiveErrorFocusColor": p["errorInactive"],
        "warningFocusColor": p["warning"],
        "inactiveWarningFocusColor": p["warningInactive"],
        "infoForeground": p["fgMuted"],
    }
    ui["Counter"] = {"background": p["cyan"], "foreground": p["counterFg"]}
    ui["Debugger"] = {
        "EvaluateExpression.background": p["island"],
        "Variables": {
            "valueForeground": p["green"],
            "changedValueForeground": p["cyan"],
            "modifyingValueForeground": p["cyan"],
            "collectingDataForeground": p["fgMuted"],
            "evaluatingExpressionForeground": p["fgMuted"],
            "errorMessageForeground": p["red"],
            "exceptionForeground": p["red"],
            "typeForeground": p["purple"],
        },
    }
    ui["DragAndDrop"] = {
        "areaBackground": mix(p["island"], p["magenta"], 0.12),
        "borderColor": p["magenta"],
        "rowBackground": alpha(p["magenta"], 0.2),
    }
    ui["Editor"] = {
        "background": p["island"],
        "foreground": p["fg"],
        "shortcutForeground": p["cyan"],
        "SearchField": {"background": p["surface"], "borderColor": p["border"]},
        "Toolbar.borderColor": p["border"],
        "ToolTip": {
            "background": p["popup"],
            "border": p["border"],
            "selectionBackground": p["selection"],
            "iconHoverBackground": p["hover"],
            "errorBackground": p["errorSoftBg"],
            "errorBorder": p["red"],
            "warningBackground": p["warningSoftBg"],
            "warningBorder": p["warning"],
            "successBackground": mix(p["island"], p["green"], 0.16),
            "successBorder": p["green"],
        },
    }
    ui["EditorTabs"] = {
        "background": p["island"],
        "hoverBackground": p["hover"],
        "hoverInactiveBackground": p["hover"],
        "underlinedTabBackground": p["island"],
        "inactiveUnderlinedTabBackground": p["island"],
        "inactiveColoredFileBackground": alpha(p["magenta"], 0.12),
        "underlinedTabForeground": p["magenta"],
        "underlinedBorderColor": p["magenta"],
        "underlineColor": p["magenta"],
        "inactiveUnderlinedTabBorderColor": p["fgMuted"],
        "inactiveUnderlineColor": p["fgMuted"],
        "underTabsBorderColor": p["island"],
        "underlineHeight": 3,
    }
    ui["FileColor"] = {
        "Blue": mix(p["island"], p["cyan"], 0.16),
        "Green": mix(p["island"], p["green"], 0.16),
        "Orange": mix(p["island"], p["orange"], 0.16),
        "Rose": mix(p["island"], p["magenta"], 0.16),
        "Violet": mix(p["island"], p["purple"], 0.16),
        "Yellow": mix(p["island"], p["gold"], 0.16),
        "Gray": p["elevated"],
    }
    ui["FlameGraph"] = {
        "JavaCell": flame_cell(p, "cyan"),
        "ParentCell": flame_cell(p, "purple"),
        "NativeCell": flame_cell(p, "gold"),
    }
    ui["GotItTooltip"] = {
        "background": p["popup"],
        "foreground": p["fg"],
        "borderColor": p["border"],
        "animationBackground": p["elevated"],
        "codeBackground": p["surface"],
        "codeBorderColor": p["border"],
        "codeForeground": p["cyan"],
        "Header.foreground": p["fg"],
        "iconBorderColor": p["magenta"],
        "iconFillColor": mix(p["island"], p["magenta"], 0.2),
        "imageBorderColor": p["border"],
        "linkForeground": p["cyan"],
        "linkUnderlineDefaultColor": alpha(p["cyan"], 0.0),
        "linkUnderlineHoveredColor": p["cyan"],
        "secondaryActionForeground": p["fgMuted"],
        "shortcutBackground": p["elevated"],
        "shortcutForeground": p["fgMuted"],
        "stepForeground": p["fgMuted"],
        "Button": {
            "startBackground": p["magenta"],
            "endBackground": p["purple"],
            "startBorderColor": p["magenta"],
            "endBorderColor": p["purple"],
            "contrastBackground": p["magenta"],
            "foreground": "#FFFFFF",
        },
    }
    ui["Ide"] = {"Shadow": shadow_ring(p)}
    ui["Label"] = {
        "foreground": p["fg"],
        "disabledForeground": p["fgDisabled"],
        "infoForeground": p["fgMuted"],
        "errorForeground": p["red"],
        "successForeground": p["green"],
        "selectedForeground": p["magenta"],
    }
    ui["LineProfiler"] = {
        "Line": {
            "labelBackground": p["elevated"],
            "foreground": p["fgMuted"],
            "hoverBackground": p["selection"],
        },
        "HotLine": {
            "labelBackground": mix(p["island"], p["red"], 0.24),
            "foreground": p["red"],
            "hoverBackground": mix(p["island"], p["red"], 0.34),
        },
        "IgnoredLine": {
            "labelBackground": p["elevated"],
            "foreground": p["fgDisabled"],
        },
    }
    ui["Link"] = {
        "activeForeground": p["cyan"],
        "hoverForeground": p["cyan"],
        "visitedForeground": p["purple"],
        "pressedForeground": p["magenta"],
    }
    ui["List"] = {
        "background": p["island"],
        "selectionBackground": p["selection"],
        "selectionForeground": p["fg"],
        "selectionInactiveBackground": p["hover"],
        "selectionInactiveForeground": p["fg"],
        "hoverBackground": p["hover"],
        "hoverInactiveBackground": p["hover"],
        "Tag.background": mix(p["island"], p["purple"], 0.2),
        "Tag.foreground": p["purple"],
    }
    ui["MainToolbar"] = {
        "background": p["canvas"],
        "inactiveBackground": p["canvas"],
        "Dropdown": {
            "hoverBackground": p["chromeHover"],
            "pressedBackground": p["chromePressed"],
        },
        "Icon": {
            "hoverBackground": p["chromeHover"],
            "pressedBackground": p["chromePressed"],
        },
    }
    ui["MainWindow"] = {
        "FullScreeControl.Background": p["chromePressed"],
        "Tab": {
            "background": p["canvas"],
            "foreground": p["fgMuted"],
            "hoverBackground": p["chromeHover"],
            "selectedBackground": p["island"],
            "selectedForeground": p["fg"],
            "separatorColor": p["border"],
        },
    }
    ui["MemoryIndicator"] = {
        "allocatedBackground": p["elevated"],
        "usedBackground": mix(p["island"], p["cyan"], 0.4),
    }
    ui["Menu"] = {
        "background": p["popup"],
        "borderColor": p["border"],
        "separatorColor": p["elevated"],
    }
    ui["MenuItem"] = {
        "background": p["popup"],
        "selectionBackground": p["selection"],
        "selectionForeground": p["fg"],
        "acceleratorForeground": p["fgMuted"],
    }
    ui["Notification"] = {
        "background": p["popup"],
        "borderColor": p["border"],
        "foreground": p["fg"],
        "errorBackground": p["popup"],
        "errorBorderColor": p["red"],
        "errorForeground": p["fg"],
        "Shadow": shadow_ring(p),
        "ToolWindow": {
            "informativeBackground": p["popup"],
            "informativeBorderColor": p["cyan"],
            "informativeForeground": p["fg"],
            "warningBackground": p["popup"],
            "warningBorderColor": p["warning"],
            "warningForeground": p["fg"],
            "errorBackground": p["popup"],
            "errorBorderColor": p["red"],
            "errorForeground": p["fg"],
        },
    }
    ui["Plugins"] = {
        "hoverBackground": p["hover"],
        "SectionHeader.background": p["island"],
        "tagBackground": mix(p["island"], p["cyan"], 0.2),
        "tagForeground": p["cyan"],
        "eapTagBackground": mix(p["island"], p["purple"], 0.2),
        "paidTagBackground": mix(p["island"], p["gold"], 0.2),
        "trialTagBackground": mix(p["island"], p["green"], 0.2),
        "Button": {
            "installBorderColor": p["green"],
            "installForeground": p["green"],
            "installFillBackground": p["green"],
            "installFillForeground": p["counterFg"],
            "updateBackground": p["magenta"],
            "updateBorderColor": p["magenta"],
            "updateForeground": "#FFFFFF",
        },
    }
    ui["Popup"] = {
        "background": p["popup"],
        "borderColor": p["border"],
        "separatorColor": p["elevated"],
        "Header": {
            "activeBackground": p["popup"],
            "inactiveBackground": p["popup"],
        },
        "Advertiser": {
            "background": p["popup"],
            "foreground": p["fgMuted"],
            "borderColor": p["elevated"],
        },
    }
    ui["PresentationAssistant"] = {
        "Bright.PopupBackground": p["popup"],
        "Bright.Popup.border": p["magenta"],
        "Bright.Popup.foreground": p["fg"],
        "Bright.keymapLabel": p["fgMuted"],
        "Pale.PopupBackground": p["popup"],
        "Pale.Popup.border": p["border"],
        "Pale.Popup.foreground": p["fgMuted"],
        "Pale.keymapLabel": p["fgDisabled"],
    }
    ui["Profiler"] = {
        "ChartSlider": {"foreground": p["fgMuted"], "lineColor": p["borderStrong"]},
        "CpuChart": {
            "background": alpha(p["cyan"], 0.28),
            "borderColor": p["cyan"],
            "inactiveBackground": alpha(p["cyan"], 0.12),
            "inactiveBorderColor": mix(p["island"], p["cyan"], 0.5),
            "pointBackground": p["island"],
            "pointBorderColor": p["cyan"],
        },
        "MemoryChart": {
            "background": alpha(p["magenta"], 0.28),
            "borderColor": p["magenta"],
            "inactiveBackground": alpha(p["magenta"], 0.12),
            "inactiveBorderColor": mix(p["island"], p["magenta"], 0.5),
            "pointBackground": p["island"],
            "pointBorderColor": p["magenta"],
        },
        "LiveChart.horizontalAxisColor": p["border"],
        "Timer": {
            "foreground": p["fg"],
            "disabledForeground": p["fgDisabled"],
            "background": p["elevated"],
        },
    }
    ui["ProgressBar"] = {
        "trackColor": p["elevated"],
        "progressColor": p["magenta"],
        "indeterminateStartColor": p["magenta"],
        "indeterminateEndColor": p["cyan"],
        "failedColor": p["red"],
        "failedEndColor": p["errorInactive"],
        "failedendcolor": p["errorInactive"],
        "passedColor": p["green"],
        "passedEndColor": p["addedLine"],
        "passedendcolor": p["addedLine"],
    }
    ui["ProjectGradients"] = project_gradients(p)
    ui["Recap"] = {
        "cardBackground": p["island"],
        "cardBorderColor": p["border"],
        "cardHeaderTitle": p["fg"],
        "cardHeaderLinkBackground": p["elevated"],
        "cardHeaderSubtitle": p["fgMuted"],
        "cardFooterForeground": p["fgMuted"],
        "chroniclesBackground": p["surface"],
        "chroniclesBackgroundHovered": p["hover"],
        "chroniclesBorderColor": p["border"],
        "chroniclesForeground": p["fg"],
        "chroniclesForegroundGreyed": p["fgMuted"],
        "errorBackground": p["errorSoftBg"],
        "errorBorder": p["red"],
        "errorForeground": p["red"],
    }
    ui["RecentProject"] = recent_projects(p)
    ui["Review"] = {
        "Branch.Background": mix(p["island"], p["cyan"], 0.2),
        "Branch.Background.Hover": mix(p["island"], p["cyan"], 0.3),
        "ChatItem.Hover": p["hover"],
        "State.Background": mix(p["island"], p["purple"], 0.2),
        "State.Foreground": p["purple"],
    }
    ui["RunWidget"] = {
        "foreground": p["fg"],
        "hoverBackground": p["chromeHover"],
        "pressedBackground": p["chromePressed"],
        "iconColor": p["fg"],
        "runIconColor": p["green"],
        "runningBackground": mix(p["canvas"], p["green"], 0.28),
        "runningIconColor": p["green"],
        "stopBackground": mix(p["canvas"], p["red"], 0.28),
    }
    ui["ScrollBar"] = {
        "thumbColor": p["scrollThumb"],
        "thumbBorderColor": p["scrollThumb"],
        "hoverThumbColor": p["scrollThumbHover"],
        "hoverThumbBorderColor": p["scrollThumbHover"],
        "trackColor": alpha(p["canvas"], 0.0),
        "hoverTrackColor": alpha(p["canvas"], 0.0),
        "Mac": {
            "thumbColor": p["scrollThumb"],
            "thumbBorderColor": p["scrollThumb"],
            "hoverThumbColor": p["scrollThumbHover"],
            "hoverThumbBorderColor": p["scrollThumbHover"],
        },
        "Transparent": {
            "thumbColor": p["scrollThumb"],
            "thumbBorderColor": p["scrollThumb"],
            "hoverThumbColor": p["scrollThumbHover"],
            "hoverThumbBorderColor": p["scrollThumbHover"],
            "trackColor": alpha(p["canvas"], 0.0),
            "hoverTrackColor": alpha(p["canvas"], 0.0),
        },
    }
    ui["SearchEverywhere"] = {
        "SearchField": {"background": p["surface"], "borderColor": p["border"]},
        "Tab": {"selectedBackground": p["selection"], "selectedForeground": p["magenta"]},
    }
    ui["SearchMatch"] = {
        "startBackground": p["goldSoft"],
        "endBackground": p["goldSoft"],
    }
    ui["Slider"] = {
        "buttonColor": p["magenta"],
        "buttonBorderColor": p["magenta"],
        "tickColor": p["fgMuted"],
        "trackColor": p["elevated"],
    }
    ui["StatusBar"] = {
        "background": p["canvas"],
        "hoverBackground": p["chromeHover"],
        "Widget": {
            "hoverBackground": p["chromeHover"],
            "pressedBackground": p["chromePressed"],
        },
        "Breadcrumbs": {
            "foreground": p["fgMuted"],
            "hoverBackground": p["hover"],
            "hoverForeground": p["fg"],
            "pressedBackground": p["selection"],
            "selectionBackground": p["selection"],
            "selectionInactiveBackground": p["hover"],
        },
    }
    ui["TabbedPane"] = {
        "underlineColor": p["magenta"],
        "hoverColor": p["hover"],
        "focusColor": p["selection"],
        "disabledForeground": p["fgDisabled"],
    }
    ui["Table"] = {
        "background": p["island"],
        "selectionBackground": p["selection"],
        "selectionForeground": p["fg"],
        "selectionInactiveBackground": p["hover"],
        "selectionInactiveForeground": p["fg"],
        "gridColor": p["elevated"],
        "hoverBackground": p["hover"],
        "stripeColor": p["stripe"],
    }
    ui["TableHeader"] = {
        "background": p["island"],
        "bottomSeparatorColor": p["elevated"],
        "separatorColor": p["elevated"],
    }
    ui["TextArea"] = {
        "background": p["surface"],
        "selectionBackground": p["selection"],
        "inactiveForeground": p["fgDisabled"],
    }
    ui["TextField"] = {
        "background": p["surface"],
        "selectionBackground": p["selection"],
        "selectionForeground": p["selectionFg"],
        "inactiveForeground": p["fgDisabled"],
    }
    ui["TitlePane"] = {
        "background": p["canvas"],
        "inactiveBackground": p["canvas"],
        "infoForeground": p["fgMuted"],
        "inactiveInfoForeground": p["fgDisabled"],
    }
    ui["ToolTip"] = {
        "background": p["elevated"] if p["dark"] else p["surface"],
        "foreground": p["fg"],
        "borderColor": p["magenta"],
        "shortcutForeground": p["cyan"],
    }
    ui["Tooltip"] = {
        "Learning": {
            "background": p["popup"],
            "foreground": p["fg"],
            "borderColor": p["magenta"],
            "codeBorderColor": p["border"],
            "Header.foreground": p["fg"],
            "iconBorderColor": p["magenta"],
            "iconFillColor": mix(p["island"], p["magenta"], 0.2),
            "linkForeground": p["cyan"],
            "linkUnderlineDefaultColor": alpha(p["cyan"], 0.0),
            "linkUnderlineHoveredColor": p["cyan"],
            "secondaryActionForeground": p["fgMuted"],
            "spanBackground": p["elevated"],
            "spanForeground": p["fg"],
            "stepNumberForeground": p["fgMuted"],
        }
    }
    ui["ToolWindow"] = {
        "background": p["island"],
        "borderColor": p["island"],
        "Header": {
            "background": p["island"],
            "inactiveBackground": p["island"],
            "borderColor": p["island"],
        },
        "HeaderTab": {
            "selectedBackground": p["selection"],
            "selectedInactiveBackground": p["hover"],
            "hoverBackground": p["hover"],
            "hoverInactiveBackground": p["hover"],
            "underlineColor": p["magenta"],
            "inactiveUnderlineColor": p["fgMuted"],
            "underlineHeight": 3,
            "underlinedTabForeground": p["magenta"],
        },
        "Button": {
            "selectedBackground": p["selection"],
            "selectedForeground": p["magenta"],
            "hoverBackground": p["chromeHover"],
            "foreground": p["fgMuted"],
            "DragAndDrop": {
                "buttonDropBackground": mix(p["canvas"], p["magenta"], 0.2),
                "buttonDropBorderColor": p["magenta"],
                "buttonFloatingBackground": p["elevated"],
            },
        },
        "DragAndDrop.areaBackground": mix(p["island"], p["magenta"], 0.12),
        "Stripe": {"background": p["canvas"], "separatorColor": p["border"]},
    }
    ui["Tree"] = {
        "background": p["island"],
        "foreground": p["fg"],
        "selectionBackground": p["selection"],
        "selectionForeground": p["fg"],
        "selectionInactiveBackground": p["hover"],
        "selectionInactiveForeground": p["fg"],
        "hoverBackground": p["hover"],
        "rowHeight": 24,
    }
    ui["TrialWidget"] = {
        state: {
            "foreground": foreground,
            "background": background,
            "borderColor": border,
            "hoverForeground": foreground,
            "hoverBackground": hover,
            "hoverBorderColor": border,
        }
        for state, foreground, background, border, hover in (
            (
                "Default",
                p["fgMuted"],
                alpha(p["canvas"], 0.0),
                alpha(p["canvas"], 0.0),
                p["chromeHover"],
            ),
            (
                "Active",
                p["cyan"],
                mix(p["canvas"], p["cyan"], 0.16),
                mix(p["canvas"], p["cyan"], 0.32),
                mix(p["canvas"], p["cyan"], 0.24),
            ),
            (
                "Alert",
                p["red"],
                mix(p["canvas"], p["red"], 0.16),
                mix(p["canvas"], p["red"], 0.32),
                mix(p["canvas"], p["red"], 0.24),
            ),
            (
                "Expiring",
                p["warning"],
                mix(p["canvas"], p["warning"], 0.16),
                mix(p["canvas"], p["warning"], 0.32),
                mix(p["canvas"], p["warning"], 0.24),
            ),
            (
                "Progress",
                p["magenta"],
                mix(p["canvas"], p["magenta"], 0.16),
                mix(p["canvas"], p["magenta"], 0.32),
                mix(p["canvas"], p["magenta"], 0.24),
            ),
        )
    }
    ui["ValidationTooltip"] = {
        "errorBackground": p["errorSoftBg"],
        "errorBorderColor": p["red"],
        "warningBackground": p["warningSoftBg"],
        "warningBorderColor": p["warning"],
    }
    ui["VersionControl"] = {
        "GitLog": {
            "headIconColor": p["magenta"],
            "localBranchIconColor": p["green"],
            "remoteBranchIconColor": p["cyan"],
            "tagIconColor": p["gold"],
            "otherIconColor": p["purple"],
        },
        "Log": {
            "Commit": {
                "currentBranchBackground": p["stripe"],
                "hoveredBackground": p["hover"],
                "unmatchedForeground": p["fgDisabled"],
                "Reference.foreground": p["fgMuted"],
            }
        },
        "FileHistory.Commit.selectedBranchBackground": p["selection"],
        "MarkerPopup.borderColor": p["border"],
        "Merge.Status.NoConflicts.foreground": p["green"],
        "RefLabel": {
            "backgroundBase": p["selection"] if p["dark"] else p["elevated"],
            "foreground": p["fg"],
        },
    }
    ui["WelcomeScreen"] = {
        "Projects": {
            "selectionBackground": p["selection"],
            "selectionInactiveBackground": p["hover"],
            "actions.background": p["surface"],
        },
        "Banner.background": mix(p["canvas"], p["purple"], 0.2),
        "Details.background": p["island"],
        "SidePanel.background": p["canvas"],
    }
    return ui


def icons(p):
    suffix = ".Dark" if p["dark"] else ""
    palette = {
        "Checkbox.Background.Default": p["surface"],
        "Checkbox.Border.Default": p["fgMuted"] if p["dark"] else p["fgDisabled"],
        "Checkbox.Background.Selected": p["magenta"],
        "Checkbox.Border.Selected": p["magenta"],
        "Checkbox.Foreground.Selected": "#FFFFFF",
        "Checkbox.Focus.Wide": p["cyan"],
        "Checkbox.Background.Disabled": p["surface"] if p["dark"] else p["stripe"],
        "Checkbox.Border.Disabled": p["border"],
        "Checkbox.Foreground.Disabled": p["fgDisabled"],
        f"Actions.Blue{suffix}": p["cyan"],
        f"Actions.Green{suffix}": p["green"],
        f"Actions.Red{suffix}": p["red"],
        f"Actions.Yellow{suffix}": p["gold"],
        f"Actions.Grey{suffix}": p["fgMuted"],
        f"Actions.GreyInline{suffix}": p["fgMuted"],
        "Objects.Grey": p["fgMuted"],
        "Objects.Blue": p["cyan"],
        "Objects.Green": p["green"],
        "Objects.Yellow": p["gold"],
        "Objects.YellowDark": p["orange"],
        "Objects.Purple": p["purple"],
        "Objects.Pink": p["magenta"],
        "Objects.Red": p["red"],
        "Objects.RedStatus": p["red"],
        "Objects.GreenAndroid": p["green"],
        "Objects.BlackText": p["counterFg"],
    }
    return {"ColorPalette": palette}
