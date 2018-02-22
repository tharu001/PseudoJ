package org.ide.pseudoj.core;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.EditorReference;

public class WindowOrganizer {
	private static boolean isOrganized = false;
	private static String projName;

	public static void setProjName(String name) {
		projName = name;
	}

	public static void organize() {
		if (!isOrganized) {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			page.addPartListener(new IPartListener2() {
				@Override
				public void partActivated(IWorkbenchPartReference partRef) {

				}

				@Override
				public void partBroughtToTop(IWorkbenchPartReference partRef) {
					// TODO Auto-generated method stub

				}

				@Override
				public void partClosed(IWorkbenchPartReference partRef) {
					// TODO Auto-generated method stub

				}

				@Override
				public void partDeactivated(IWorkbenchPartReference partRef) {
					// TODO Auto-generated method stub

				}

				@Override
				public void partOpened(IWorkbenchPartReference partRef) {
					page.removePartListener(this);
					for (IEditorReference ef : page.getEditorReferences()) {
						IEditorPart ip = ef.getEditor(true);
						IFileEditorInput input = (IFileEditorInput) ip.getEditorInput();
						IFile file = input.getFile();
						IProject proj = file.getProject();
						projName = proj.getName();
						if (!ef.getName().equals(partRef.getPartName())) {
							page.closeEditor(ip, true);
						}
					}
					IWorkspace workspace = ResourcesPlugin.getWorkspace();
					try {
						if (partRef.getPartName().split("\\.")[1].equals("psj")) {
							IFile javaFile = workspace.getRoot().getFile(new Path("/" + projName
									+ "/src/defaultPackage/" + partRef.getPartName().split("\\.")[0] + ".java"));
							try {
								IDE.openEditor(page, javaFile, false);
							} catch (PartInitException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						} else if (partRef.getPartName().split("\\.")[1].equals("java")) {
							IFile psjFile = workspace.getRoot().getFile(
									new Path("/" + projName + "/" + partRef.getPartName().split("\\.")[0] + ".psj"));
							try {
								IDE.openEditor(page, psjFile, false);
							} catch (PartInitException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					} catch (ArrayIndexOutOfBoundsException ai) {
						// non psj or java file accessed
					}
					page.addPartListener(this);
				}

				@Override
				public void partHidden(IWorkbenchPartReference partRef) {
					// TODO Auto-generated method stub

				}

				@Override
				public void partVisible(IWorkbenchPartReference partRef) {
					// TODO Auto-generated method stub

				}

				@Override
				public void partInputChanged(IWorkbenchPartReference partRef) {
					// TODO Auto-generated method stub

				}

			});
			isOrganized = true;
		}
	}
}
