package tw.ttucse.cloudhw3.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.jdo.Extent;
import javax.jdo.JDOUserException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;

import tw.ttucse.cloudhw3.client.FileService;
import tw.ttucse.cloudhw3.client.MyFile;
import tw.ttucse.cloudhw3.client.PMF;
import tw.ttucse.cloudhw3.client.ShareLink;
import tw.ttucse.cloudhw3.client.User;
import tw.ttucse.cloudhw3.client.File.FileType;

import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class FileServiceImpl extends RemoteServiceServlet implements
		FileService {
	BlobstoreService blobstoreService = BlobstoreServiceFactory
			.getBlobstoreService();

	public FileServiceImpl() {
		System.out.println("fileserviceImpl loading scuess");
	}

	private static final long serialVersionUID = 8169459258144511133L;

	@Override
	public MyFile getFileWithName(String name, String parent)
			throws IllegalArgumentException {
		return getFileWithName(name, parent, false);
	}

	@Override
	public MyFile getFileWithName(String name, String parent, boolean isSure)
			throws IllegalArgumentException {
		PersistenceManager pm = PMF.getInstance().getPersistenceManager();

		String queryString = "name ==\'" + name + "\' && fileFolder == \'"
				+ parent + "\'";
		List<MyFile> files;
		do {
			@SuppressWarnings("unchecked")
			List<MyFile> list = (List<MyFile>) pm.newQuery(MyFile.class,
					queryString).execute();
			files = list;
			isSure = isSure && files.isEmpty();
		} while (isSure);

		return files.isEmpty() ? null : files.get(0);
	}

	@Override
	public Long getNewID() throws IllegalArgumentException {
		PersistenceManager pm = PMF.getInstance().getPersistenceManager();
		String query2 = "SELECT MAX(id) FROM " + MyFile.class.getName();
		Long ID = (Long) pm.newQuery(query2).execute();
		if (ID == null) {
			ID = new Long(1);
		}
		pm.close();
		return ID + 1;
	}

	@Override
	public MyFile getFileWithID(Long ID) throws IllegalArgumentException {
		return getFileWithID(ID, false);
	}

	@Override
	public MyFile getFileWithID(Long ID, boolean isSure)
			throws IllegalArgumentException {
		PersistenceManager pm = PMF.getInstance().getPersistenceManager();

		String queryString = "id ==" + ID;
		List<MyFile> files;
		do {
			@SuppressWarnings("unchecked")
			List<MyFile> list = (List<MyFile>) pm.newQuery(MyFile.class,
					queryString).execute();
			files = list;
			isSure = isSure && files.isEmpty();
		} while (isSure);

		return files.isEmpty() ? null : files.get(0);
	}
	


	@Override
	public MyFile[] getFileWithID(Long[] IDs) throws IllegalArgumentException {
		ArrayList<MyFile> fileArrayList = new ArrayList<MyFile>();
		for (Long id : IDs) {
			fileArrayList.add(getFileWithID(id));
		}
		return fileArrayList.toArray(new MyFile[fileArrayList.size()]);
	}

	@Override
	public MyFile[] getFilesWithParent(String parent)
			throws IllegalArgumentException {
		PersistenceManager pm = PMF.getInstance().getPersistenceManager();

		String queryString = "fileFolder == \'" + parent + "\'";
		@SuppressWarnings("unchecked")
		List<MyFile> files = (List<MyFile>) pm.newQuery(MyFile.class,
				queryString).execute();

		return files.toArray(new MyFile[files.size()]);
	}

	@Override
	public MyFile[] getParents() throws IllegalArgumentException {
		PersistenceManager pm = PMF.getInstance().getPersistenceManager();
		String queryString = "SELECT FROM " + MyFile.class.getName();
		try {
			@SuppressWarnings("unchecked")
			List<MyFile> dirs = (List<MyFile>) pm.newQuery(queryString)
					.execute();
			return dirs.toArray(new MyFile[dirs.size()]);
		} finally {
			pm.close();
		}
	}

	@Override
	public MyFile createFolder(String parent, String name)
			throws IllegalArgumentException {
		if (name.equals("")) {
			System.out.println("Create not OK!");
			return null;
		}
		PersistenceManager pm = PMF.getInstance().getPersistenceManager();
		try {
			Long ID = getNewID();
			String query = "name == \'" + name + "\' && fileFolder == \'"
					+ parent + "\'";

			@SuppressWarnings("unchecked")
			List<MyFile> dirs = (List<MyFile>) pm.newQuery(MyFile.class, query)
					.execute();
			if (dirs.size() > 1) {
				System.out.println("Create not OK!");
				return null;
			} else {
				if (dirs.size() ==1 && dirs.get(0).getType() == FileType.DIR) {
					System.out.println("Create not OK!");
					return null;					
				} else {
					MyFile dir = new MyFile(name, null, parent, FileType.DIR);
					dir.setId(ID);
					pm.makePersistent(dir);
					pm.flush();
					System.out.println("Create folder OK!");
					return dir;
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			pm.close();
		}

	}

	@Override
	public MyFile deleteFile(MyFile file) throws IllegalArgumentException {
		PersistenceManager pm = PMF.getInstance().getPersistenceManager();
		Transaction transaction = pm.currentTransaction();
		String filepath = file.getFileFolder() + "/" + file.getName();
		Object obj = null;
		try {
			Extent<MyFile> ext = pm.getExtent(MyFile.class, false);
			String str = "id==" + file.getId();
			Query qry = pm.newQuery(ext, str);
			Collection<?> c = (Collection<?>) qry.execute();
			obj = c.iterator().next();
			transaction.begin();
			pm.deletePersistent(obj);
			transaction.commit();
			System.out.println("Delete Path : " + filepath);
		} catch (Exception ex) {
			transaction.rollback();
			throw ex;
		}
		return (MyFile) obj;
	}

	@Override
	public boolean deleteFloder(MyFile file) throws IllegalArgumentException {
		if (file.getType() == FileType.FILE) {
			return false;
		}
		String path = file.getFileFolder() + "/" + file.getName() + "/";
		PersistenceManager pm = PMF.getInstance().getPersistenceManager();
		MyFile[] myFilesattay;
		String queryString = "SELECT FROM " + MyFile.class.getName();
		try {
			@SuppressWarnings("unchecked")
			List<MyFile> dirs = (List<MyFile>) pm.newQuery(queryString)
					.execute();
			myFilesattay = dirs.toArray(new MyFile[dirs.size()]);
			ArrayList<MyFile> myFileslist = new ArrayList<MyFile>();
			for (MyFile myFile : myFilesattay) {
				if ((myFile.getFileFolder() + "/").startsWith(path)) {
					myFileslist.add(myFile);
				}
			}
			myFileslist.add(file);
			for (MyFile myFile : myFileslist) {
				deleteFile(myFile);
			}
		} finally {
			pm.close();
		}
		return true;
	}

	@Override
	public void checkIfDefaultFileExist(User user) {
		PersistenceManager pm = PMF.getInstance().getPersistenceManager();
		System.out.println("Check If Default File Exist");
		String query = "name==\'" + user.getAccount() + "\' && fileFolder==\'"
				+ "." + "\'";
		@SuppressWarnings("unchecked")
		List<MyFile> myFiles = (List<MyFile>) pm.newQuery(MyFile.class, query)
				.execute();
		if (myFiles.isEmpty()) {
			System.out.println("Default myFiles not exist, add folder....");
			MyFile newFile = new MyFile(user.getAccount(), null, ".",
					FileType.DIR);
			String query2 = "SELECT MAX(id) FROM " + MyFile.class.getName();
			Long ID = (Long) pm.newQuery(query2).execute();
			if (ID == null) {
				ID = new Long(1);
			}
			newFile.setId(ID);
			try {
				pm.makePersistent(newFile);
				pm.flush();
			} finally {
				pm.close();
			}
		} else {
			System.out.println("Default file exist");
		}
		System.out.println("Done\n");
	}

	@Override
	public MyFile editMyFile(MyFile newfile) throws IllegalArgumentException {
		PersistenceManager pm = PMF.getInstance().getPersistenceManager();
		Transaction transaction = pm.currentTransaction();
		MyFile oldfile = null;
		String oldParentString = null;
		String newParentString = newfile.getFileFolder() + "/"
				+ newfile.getName() + "/";

		try {
			Extent<MyFile> ext = pm.getExtent(MyFile.class, false);
			String str = "id==" + newfile.getId();
			Query qry = pm.newQuery(ext, str);
			Collection<?> c = (Collection<?>) qry.execute();
			Object obj = c.iterator().next();
			oldfile = (MyFile) obj;
			oldParentString = oldfile.getFileFolder() + "/" + oldfile.getName()
					+ "/";

			oldfile.setName(newfile.getName());
			oldfile.setFileFolder(newfile.getFileFolder());

			transaction.begin();
			pm.makePersistent(obj);
			transaction.commit();
			System.out.println("Modify user success.");
		} catch (JDOUserException ex) {
			ex.printStackTrace();
			transaction.rollback();

		} finally {
			pm.close();
		}

		modifySubFloder(oldParentString, newParentString);
		return oldfile;
	}

	public void modifySubFloder(String oldFolderString, String newFolderString) {
		PersistenceManager pm = PMF.getInstance().getPersistenceManager();
		MyFile[] myFilesattay;
		String queryString = "SELECT FROM " + MyFile.class.getName();
		try {
			@SuppressWarnings("unchecked")
			List<MyFile> dirs = (List<MyFile>) pm.newQuery(queryString)
					.execute();
			myFilesattay = dirs.toArray(new MyFile[dirs.size()]);
			ArrayList<MyFile> myFileslist = new ArrayList<MyFile>();
			int oldFolderStringLen = oldFolderString.length();
			for (MyFile myFile : myFilesattay) {
				if ((myFile.getFileFolder() + "/").startsWith(oldFolderString)) {
					myFileslist.add(myFile);
				}
			}
			Transaction transaction = pm.currentTransaction();

			for (MyFile myFile : myFileslist) {
				String folderString = myFile.getFileFolder() + "/";
				folderString = newFolderString
						+ folderString.substring(oldFolderStringLen);
				folderString = folderString.substring(0,
						folderString.length() - 1);
				System.out.println("Modify : " + myFile.getFileFolder() + "/"
						+ myFile.getName() + " => " + folderString + "/"
						+ myFile.getName());
				myFile.setFileFolder(folderString);

				try {
					transaction.begin();
					pm.makePersistent(myFile);
					transaction.commit();
				} catch (Exception ex) {
					ex.printStackTrace();
					transaction.rollback();
				}
			}
		} finally {
			pm.close();
		}
	}

	@Override
	public String getUploadURL() throws IllegalArgumentException {
		String url = blobstoreService.createUploadUrl("/cloudapphw3/upload");
		return url;
	}

	@Override
	public ShareLink addFileToShareLink(Long[] fileId, String shareLinkName,
			String owner) throws IllegalArgumentException {
		PersistenceManager pm = PMF.getInstance().getPersistenceManager();
		String queryString = "SELECT FROM " + ShareLink.class.getName()
				+ " WHERE shareName == \'" + shareLinkName
				+ "\' && owner == \'" + owner + "\'";
		Query query = pm.newQuery(queryString);

		@SuppressWarnings("unchecked")
		List<ShareLink> shareLinks = (List<ShareLink>) query.execute();
		if (shareLinks.size() == 0) {
			// share link not exist

			ShareLink shareLink = new ShareLink(shareLinkName, owner);
			for (Long id : fileId) {
				shareLink.addID(id);
			}

			try {
				pm.makePersistent(shareLink);
				pm.flush();
				return shareLink;
			} finally {
				pm.close();
			}

		} else {
			// share link exist
			ShareLink shareLink = shareLinks.get(0);
			for (Long id : fileId) {
				shareLink.addID(id);
			}
			System.out.println(shareLink);
			Transaction transaction = pm.currentTransaction();
			transaction.begin();
			try {
				pm.makePersistent(shareLink);
				pm.flush();
				transaction.commit();
				return shareLink;
			} catch (Exception ex) {
				transaction.rollback();
				ex.printStackTrace();
				return null;
			} finally {
				pm.close();
			}

		}
	}

	@Override
	public ShareLink[] getShareLinks() throws IllegalArgumentException {
		PersistenceManager pm = PMF.getInstance().getPersistenceManager();
		String queryString = "SELECT FROM " + ShareLink.class.getName();
		try {
			@SuppressWarnings("unchecked")
			List<ShareLink> dirs = (List<ShareLink>) pm.newQuery(queryString)
					.execute();
			return dirs.toArray(new ShareLink[dirs.size()]);
		} finally {
			pm.close();
		}
	}

	@Override
	public ShareLink[] getShareLinksWithOwner(String owner)
			throws IllegalArgumentException {
		PersistenceManager pm = PMF.getInstance().getPersistenceManager();

		String queryString = "owner == \'" + owner + "\'";
		@SuppressWarnings("unchecked")
		List<ShareLink> files = (List<ShareLink>) pm.newQuery(ShareLink.class,
				queryString).execute();

		return files.toArray(new ShareLink[files.size()]);
	}

	@Override
	public ShareLink[] getShareLinksWithShareName(String keyword)
			throws IllegalArgumentException {

		PersistenceManager pm = PMF.getInstance().getPersistenceManager();

		String queryString = "shareName == \'" + keyword + "\'";
		@SuppressWarnings("unchecked")
		List<ShareLink> files = (List<ShareLink>) pm.newQuery(ShareLink.class,
				queryString).execute();

		return files.toArray(new ShareLink[files.size()]);
	}
}
