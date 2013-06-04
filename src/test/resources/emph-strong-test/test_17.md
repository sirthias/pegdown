org.spoofax.interpreter.core.InterpreterException: Exception during evaluation
at org.spoofax.interpreter.core.Interpreter.evaluate(Interpreter.java:117)
at org.spoofax.interpreter.core.Interpreter.invoke(Interpreter.java:82)
at org.strategoxt.HybridInterpreter.invoke(HybridInterpreter.java:424)
at org.strategoxt.imp.debug.core.str.launching.DebuggableHybridInterpreter.invoke(DebuggableHybridInterpreter.java:150)
at org.strategoxt.imp.runtime.services.StrategoObserver.invoke(StrategoObserver.java:701)
at org.strategoxt.imp.runtime.services.StrategoObserver.invokeSilent(StrategoObserver.java:750)
at org.strategoxt.imp.runtime.services.StrategoObserver.invokeSilent(StrategoObserver.java:741)
at org.strategoxt.imp.runtime.services.StrategoObserver.invokeSilent(StrategoObserver.java:726)
at org.strategoxt.imp.runtime.services.OnSaveService.invokeOnSave(OnSaveService.java:67)
at org.strategoxt.imp.runtime.services.OnSaveService.documentChanged(OnSaveService.java:58)
at org.eclipse.imp.editor.UniversalEditor.doSave(UniversalEditor.java:1939)
at org.eclipse.ui.texteditor.AbstractTextEditor$TextEditorSavable.doSave(AbstractTextEditor.java:7198)
at org.eclipse.ui.Saveable.doSave(Saveable.java:214)
at org.eclipse.ui.internal.SaveableHelper.doSaveModel(SaveableHelper.java:349)
at org.eclipse.ui.internal.SaveableHelper$3.run(SaveableHelper.java:195)
at org.eclipse.ui.internal.SaveableHelper$5.run(SaveableHelper.java:277)
at org.eclipse.jface.operation.ModalContext.runInCurrentThread(ModalContext.java:464)
at org.eclipse.jface.operation.ModalContext.run(ModalContext.java:372)
at org.eclipse.jface.window.ApplicationWindow$1.run(ApplicationWindow.java:759)
at org.eclipse.swt.custom.BusyIndicator.showWhile(BusyIndicator.java:70)
at org.eclipse.jface.window.ApplicationWindow.run(ApplicationWindow.java:756)
at org.eclipse.ui.internal.WorkbenchWindow.run(WorkbenchWindow.java:2649)
at org.eclipse.ui.internal.SaveableHelper.runProgressMonitorOperation(SaveableHelper.java:285)
at org.eclipse.ui.internal.SaveableHelper.runProgressMonitorOperation(SaveableHelper.java:264)
at org.eclipse.ui.internal.SaveableHelper.saveModels(SaveableHelper.java:207)
at org.eclipse.ui.internal.SaveableHelper.savePart(SaveableHelper.java:144)
at org.eclipse.ui.internal.EditorManager.savePart(EditorManager.java:1399)
at org.eclipse.ui.internal.WorkbenchPage.savePart(WorkbenchPage.java:3429)
at org.eclipse.ui.internal.WorkbenchPage.saveEditor(WorkbenchPage.java:3442)
at org.eclipse.ui.internal.handlers.SaveHandler.execute(SaveHandler.java:54)
at org.eclipse.ui.internal.handlers.HandlerProxy.execute(HandlerProxy.java:290)
at org.eclipse.core.commands.Command.executeWithChecks(Command.java:499)
at org.eclipse.core.commands.ParameterizedCommand.executeWithChecks(ParameterizedCommand.java:508)
at org.eclipse.ui.internal.handlers.HandlerService.executeCommand(HandlerService.java:169)
at org.eclipse.ui.internal.keys.WorkbenchKeyboard.executeCommand(WorkbenchKeyboard.java:468)
at org.eclipse.ui.internal.keys.WorkbenchKeyboard.press(WorkbenchKeyboard.java:786)
at org.eclipse.ui.internal.keys.WorkbenchKeyboard.processKeyEvent(WorkbenchKeyboard.java:885)
at org.eclipse.ui.internal.keys.WorkbenchKeyboard.filterKeySequenceBindings(WorkbenchKeyboard.java:567)
at org.eclipse.ui.internal.keys.WorkbenchKeyboard.access$3(WorkbenchKeyboard.java:508)
at org.eclipse.ui.internal.keys.WorkbenchKeyboard$KeyDownFilter.handleEvent(WorkbenchKeyboard.java:123)
at org.eclipse.swt.widgets.EventTable.sendEvent(EventTable.java:84)
at org.eclipse.swt.widgets.Display.filterEvent(Display.java:1262)
at org.eclipse.swt.widgets.Widget.sendEvent(Widget.java:1052)
at org.eclipse.swt.widgets.Widget.sendEvent(Widget.java:1077)
at org.eclipse.swt.widgets.Widget.sendEvent(Widget.java:1062)
at org.eclipse.swt.widgets.Widget.sendKeyEvent(Widget.java:1104)
at org.eclipse.swt.widgets.Widget.sendKeyEvent(Widget.java:1100)
at org.eclipse.swt.widgets.Widget.wmChar(Widget.java:1521)
at org.eclipse.swt.widgets.Control.WM_CHAR(Control.java:4640)
at org.eclipse.swt.widgets.Canvas.WM_CHAR(Canvas.java:345)
at org.eclipse.swt.widgets.Control.windowProc(Control.java:4528)
at org.eclipse.swt.widgets.Canvas.windowProc(Canvas.java:341)
at org.eclipse.swt.widgets.Display.windowProc(Display.java:4976)
at org.eclipse.swt.internal.win32.OS.DispatchMessageW(Native Method)
at org.eclipse.swt.internal.win32.OS.DispatchMessage(OS.java:2546)
at org.eclipse.swt.widgets.Display.readAndDispatch(Display.java:3756)
at org.eclipse.ui.internal.Workbench.runEventLoop(Workbench.java:2701)
at org.eclipse.ui.internal.Workbench.runUI(Workbench.java:2665)
at org.eclipse.ui.internal.Workbench.access$4(Workbench.java:2499)
at org.eclipse.ui.internal.Workbench$7.run(Workbench.java:679)
at org.eclipse.core.databinding.observable.Realm.runWithDefault(Realm.java:332)
at org.eclipse.ui.internal.Workbench.createAndRunWorkbench(Workbench.java:668)
at org.eclipse.ui.PlatformUI.createAndRunWorkbench(PlatformUI.java:149)
at org.eclipse.ui.internal.ide.application.IDEApplication.start(IDEApplication.java:124)
at org.eclipse.equinox.internal.app.EclipseAppHandle.run(EclipseAppHandle.java:196)
at org.eclipse.core.runtime.internal.adaptor.EclipseAppLauncher.runApplication(EclipseAppLauncher.java:110)
at org.eclipse.core.runtime.internal.adaptor.EclipseAppLauncher.start(EclipseAppLauncher.java:79)
at org.eclipse.core.runtime.adaptor.EclipseStarter.run(EclipseStarter.java:353)
at org.eclipse.core.runtime.adaptor.EclipseStarter.run(EclipseStarter.java:180)
at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
at sun.reflect.NativeMethodAccessorImpl.invoke(Unknown Source)
at sun.reflect.DelegatingMethodAccessorImpl.invoke(Unknown Source)
at java.lang.reflect.Method.invoke(Unknown Source)
at org.eclipse.equinox.launcher.Main.invokeFramework(Main.java:629)
at org.eclipse.equinox.launcher.Main.basicRun(Main.java:584)
at org.eclipse.equinox.launcher.Main.run(Main.java:1438)
at org.eclipse.equinox.launcher.Main.main(Main.java:1414)
Caused by: org.spoofax.interpreter.core.InterpreterErrorExit: Internal error: with clause failed unexpectedly in rule ‘analyze-defs’
1
at org.strategoxt.lang.InteropSDefT.evaluate(InteropSDefT.java:194)
at org.strategoxt.lang.InteropSDefT.evaluate(InteropSDefT.java:183)
at org.strategoxt.lang.InteropSDefT$StrategyBody.evaluate(InteropSDefT.java:245)
at org.strategoxt.lang.InteropSDefT$StrategyBody.eval(InteropSDefT.java:238)
at org.spoofax.interpreter.stratego.Strategy.evaluate(Strategy.java:76)
at org.spoofax.interpreter.core.Interpreter.evaluate(Interpreter.java:109)
… 76 more
Caused by: org.strategoxt.lang.StrategoErrorExit: Internal error: with clause failed unexpectedly in rule ‘analyze-defs’
1
at org.strategoxt.lang.InteropStrategy.invokeDynamic(InteropStrategy.java:60)
at org.strategoxt.lang.DynamicStrategy.invoke(DynamicStrategy.java:22)
at org.strategoxt.stratego_lib.dr_scope_1_1.invoke(dr_scope_1_1.java)
at org.strategoxt.lang.Strategy.invokeDynamic(Strategy.java:41)
at org.strategoxt.lang.InteropSDefT.evaluate(InteropSDefT.java:192)
… 81 more
Caused by: org.spoofax.interpreter.core.InterpreterErrorExit: Internal error: with clause failed unexpectedly in rule ‘analyze-defs’
1
at org.strategoxt.lang.InteropSDefT.evaluate(InteropSDefT.java:194)
at org.strategoxt.lang.InteropSDefT.evaluate(InteropSDefT.java:183)
at org.strategoxt.lang.InteropSDefT$StrategyBody.evaluate(InteropSDefT.java:245)
at org.strategoxt.lang.InteropSDefT$StrategyBody.eval(InteropSDefT.java:238)
at org.spoofax.interpreter.stratego.Strategy.evaluate(Strategy.java:76)
at org.spoofax.interpreter.stratego.SDefT.evaluate(SDefT.java:213)
at org.strategoxt.lang.InteropStrategy.invokeDynamic(InteropStrategy.java:57)
… 85 more
Caused by: org.strategoxt.lang.StrategoErrorExit: Internal error: with clause failed unexpectedly in rule ‘analyze-defs’
1
at org.strategoxt.lang.InteropStrategy.invokeDynamic(InteropStrategy.java:60)
at org.strategoxt.lang.DynamicStrategy.invoke(DynamicStrategy.java:22)
at org.strategoxt.lang.SRTS_all.map(SRTS_all.java:60)
at org.strategoxt.lang.SRTS_all.invoke(SRTS_all.java:21)
at org.strategoxt.lang.parallel.stratego_parallel.ParallelAll.invoke(ParallelAll.java:67)
at org.strategoxt.lang.compat.override.performance_tweaks.map_1_0_override.invoke(map_1_0_override.java)
at org.strategoxt.lang.Strategy.invokeDynamic(Strategy.java:40)
at org.strategoxt.lang.InteropSDefT.evaluate(InteropSDefT.java:192)
… 91 more
Caused by: org.spoofax.interpreter.core.InterpreterErrorExit: Internal error: with clause failed unexpectedly in rule ‘analyze-defs’
1
at org.strategoxt.lang.InteropSDefT.evaluate(InteropSDefT.java:194)
at org.strategoxt.lang.InteropSDefT.evaluate(InteropSDefT.java:183)
at org.strategoxt.lang.InteropSDefT$StrategyBody.evaluate(InteropSDefT.java:245)
at org.strategoxt.lang.InteropSDefT$StrategyBody.eval(InteropSDefT.java:238)
at org.spoofax.interpreter.stratego.Strategy.evaluate(Strategy.java:76)
at org.spoofax.interpreter.stratego.SDefT.evaluate(SDefT.java:213)
at org.strategoxt.lang.InteropStrategy.invokeDynamic(InteropStrategy.java:57)
… 98 more
Caused by: org.strategoxt.lang.StrategoErrorExit: Internal error: with clause failed unexpectedly in rule ‘analyze-defs’
1
at org.strategoxt.lang.compat.report_failure_compat_1_0.invoke(report_failure_compat_1_0.java:53)
at org.strategoxt.stratego_lib.report_with_failure_0_2.invoke(report_with_failure_0_2.java)
at org.strategoxt.lang.Strategy.invokeDynamic(Strategy.java:32)
at org.strategoxt.lang.InteropSDefT.evaluate(InteropSDefT.java:192)
… 104 more
Internal error evaluating strategy editor-compile-multiple
report_with_failure_0_2
analyze_defs_0_4
analyze_child_defs_0_4
a_5300
origin_track_forced_1_0
analyze_defs_0_4
analyze_child_defs_0_4
a_5300
origin_track_forced_1_0
analyze_defs_0_4
analyze_defs_0_0
map_1_0
dr_scope_1_1
analyze_top_internal_0_4
analyze_top_0_4
analyze_top_0_1
basec_to_c_h_makefile_multiple_0_0
base_c_to_compiledc_multiple_0_0
editor_compile_multiple_0_0