package com.mga.controller;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Blob;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.dcm4che3.imageio.plugins.dcm.DicomImageReadParam;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;

import com.mga.beans.DatePeriod;
import com.mga.beans.OtherDataList;
import com.mga.beans.PatientAnalysisResult;
import com.mga.beans.PatientFScore;
import com.mga.beans.PatientFunctionScoredata;
import com.mga.beans.PatientOtherDataFileList;
import com.mga.beans.PatientRestingAndMinOfActivityTime;
import com.mga.beans.PatientSoleDataFileList;
import com.mga.beans.PatientStressLevelMinExceeded;
import com.mga.beans.PatientsAnalysisResult;
import com.mga.beans.PatientsFScore;
import com.mga.beans.PatientsFunctionScoreData;
import com.mga.beans.SoleDataFilesForAnalysisInput;
import com.mga.beans.SoleDataList;
import com.mga.beans.Statistics;
import com.mga.beans.Status;
import com.mga.model.HibernateUtil;
import com.mga.model.PatientFunctionScoreData;
import com.mga.model.PatientOtherFiles;
import com.mga.model.PatientSoleDataFiles;

/*import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;*/

@Path("analysis")
public class PatientMedicalAnalysisService {

	static BufferedImage myJpegImage = null;

	Logger log = Logger.getLogger(PatientMedicalAnalysisService.class.getName());

	/**************************
	 * Sole data files upload and delete functions
	 *****************************************/

	@POST
	@Path("/uploadsoledata")
	@Consumes("multipart/form-data")
	@Produces(MediaType.APPLICATION_XML)
	public PatientsAnalysisResult uploadSoleData(FormDataMultiPart form) {

		log.debug("Sole Data upload request recieved from UI");

		Integer statusCode = 200;
		String statusMsg = null;

		String ltp = "";
		String rtp = "";
		String time = "";
		String graphdata = "";

		String inputFile = null;
		BufferedReader reader = null;
		FileInputStream fileInputStream = null;

		Timestamp firstTime = null;
		Integer timeDifference = 0;
		Integer td = 0;

		FormDataBodyPart filePart = form.getField("soledata");
		FormDataContentDisposition fileDetail = filePart.getFormDataContentDisposition();
		InputStream uploadedInputStream = filePart.getValueAs(InputStream.class);
		FormDataBodyPart idPart = form.getField("pid");
		FormDataBodyPart additionalNotesPart = form.getField("additionalnotes");
		Integer pid = idPart.getValueAs(Integer.class);
		String additionalNotes = additionalNotesPart.getValueAs(String.class);

		PatientsAnalysisResult patientsAnalysisResult = new PatientsAnalysisResult();
		PatientAnalysisResult patientAnalysisResult = new PatientAnalysisResult();
		List<PatientAnalysisResult> patientAnalysisResultList = new ArrayList<PatientAnalysisResult>();
		Status status = new Status();

		if (uploadedInputStream.equals(null) || fileDetail.equals(null)) {
			log.debug("Bad request");
			statusCode = 400;
			statusMsg = "Bad Request. Mandatory data are sent empty or null.";
		} else if (fileDetail.getFileName().equals("") || fileDetail.getFileName().equals(null)) {
			log.debug("Sole data file name is empty or null");
			statusCode = 400;
			statusMsg = "Bad Request. File name should not be empty or null.";
		} else {
			inputFile = fileDetail.getFileName();
			log.debug("Sole Data upload request recieved from UI for the patient with id " + pid);
			log.debug("Sole Data File details: \n" + "File name - " + inputFile + "\n" + "File Parameters - "
					+ fileDetail.getParameters());

			String[] fileNameType = inputFile.split("\\.");
			if (!fileNameType[fileNameType.length - 1].equals("txt")) {
				log.debug("Bad input file");
				statusCode = 400;
				statusMsg = "Unsupported input sole data file sent... Text file with extention txt required...";

				status.setCode(statusCode);
				status.setMessage(statusMsg);
				patientsAnalysisResult.setStatus(status);

				return patientsAnalysisResult;
			} else {
				Transaction transaction = null;
				Session session = null;
				try {

					String line = null;
					int nrow = 0;
					int count = 0;
					String fileDate = null;
					// String dirName = null;
					Date date = null;
					Integer ss = 0;
					long t = 0;
					Date afterAddingTenMins = null;
					Date afterAddingTenMins1 = null;

					patientsAnalysisResult.setPatientId(pid);

					reader = new BufferedReader(new InputStreamReader(uploadedInputStream));

					session = HibernateUtil.getSessionFactory().openSession();
					transaction = session.beginTransaction();
					transaction.setTimeout(5);

					while ((line = reader.readLine()) != null) {
						count++;
						if (count == 1) {
							String[] firstline = line.split("\\s+");
							fileDate = firstline[3] + "/" + firstline[4] + "/" + firstline[6] + " " + firstline[5];
							log.debug("file date - " + fileDate);
							SimpleDateFormat formatter = new SimpleDateFormat("MMM/dd/yyyy HH:mm:ss");
							date = formatter.parse(fileDate);
							Calendar soleFileDate = Calendar.getInstance();
							soleFileDate.setTime(date);
							t = soleFileDate.getTimeInMillis();
							afterAddingTenMins = new Date(t + 2000);

							/*
							 * SimpleDateFormat requiredFormat = new
							 * SimpleDateFormat("MMddyyyyHHmmss"); String
							 * folderName = requiredFormat.format(date);
							 */
						} else if (count > 2) {
							String[] fileRow = line.split("\\s+");
							if (count == 3) {
								afterAddingTenMins1 = new Date(t + ss);
								firstTime = new java.sql.Timestamp(afterAddingTenMins1.getTime());
								afterAddingTenMins1 = null;
							}
							if (count == 4) {
								timeDifference = (int) (Float.parseFloat(fileRow[0]) * 100000);
								td = (int) (Float.parseFloat(fileRow[0]) * 100);
								String hql = "UPDATE PatientSoleData set timeDifference = :timeDifference WHERE patientId = :pid AND time = :firstTime";
								Query query = session.createQuery(hql);
								query.setParameter("timeDifference", td);
								query.setParameter("pid", pid);
								query.setParameter("firstTime", firstTime);
								int result = query.executeUpdate();
							}
							ss += timeDifference;
							afterAddingTenMins1 = new Date(t + ss);
							nrow++;

							com.mga.model.PatientSoleData mPatientsoleData = new com.mga.model.PatientSoleData();

							mPatientsoleData.setTime(new java.sql.Timestamp(afterAddingTenMins1.getTime()));
							// time = time + "" + new
							// java.sql.Timestamp(afterAddingTenMins1.getTime())
							// +
							// ",";
							time = time + "" + ",";
							mPatientsoleData.setTimeDifference(td);
							mPatientsoleData.setLeftPressure0(Float.parseFloat(fileRow[1]));
							mPatientsoleData.setLeftPressure1(Float.parseFloat(fileRow[2]));
							mPatientsoleData.setLeftPressure2(Float.parseFloat(fileRow[3]));
							mPatientsoleData.setLeftPressure3(Float.parseFloat(fileRow[4]));
							mPatientsoleData.setLeftPressure4(Float.parseFloat(fileRow[5]));
							mPatientsoleData.setLeftPressure5(Float.parseFloat(fileRow[6]));
							mPatientsoleData.setLeftPressure6(Float.parseFloat(fileRow[7]));
							mPatientsoleData.setLeftPressure7(Float.parseFloat(fileRow[8]));
							mPatientsoleData.setLeftPressure8(Float.parseFloat(fileRow[9]));
							mPatientsoleData.setLeftPressure9(Float.parseFloat(fileRow[10]));
							mPatientsoleData.setLeftPressure10(Float.parseFloat(fileRow[11]));
							mPatientsoleData.setLeftPressure11(Float.parseFloat(fileRow[12]));
							mPatientsoleData.setLeftPressure12(Float.parseFloat(fileRow[13]));
							mPatientsoleData.setLeftAccelerationX(Float.parseFloat(fileRow[14]));
							mPatientsoleData.setLeftAccelerationY(Float.parseFloat(fileRow[15]));
							mPatientsoleData.setLeftAccelerationZ(Float.parseFloat(fileRow[16]));
							mPatientsoleData.setLeftTotalForce(Float.parseFloat(fileRow[17]));
							ltp = ltp + "" + fileRow[17] + ",";
							mPatientsoleData.setLeftCopX(Float.parseFloat(fileRow[18]));
							mPatientsoleData.setLeftCopY(Float.parseFloat(fileRow[19]));
							mPatientsoleData.setLeftTemperature(Float.parseFloat(fileRow[20]));
							mPatientsoleData.setRightPressure0(Float.parseFloat(fileRow[21]));
							mPatientsoleData.setRightPressure1(Float.parseFloat(fileRow[22]));
							mPatientsoleData.setRightPressure2(Float.parseFloat(fileRow[23]));
							mPatientsoleData.setRightPressure3(Float.parseFloat(fileRow[24]));
							mPatientsoleData.setRightPressure4(Float.parseFloat(fileRow[25]));
							mPatientsoleData.setRightPressure5(Float.parseFloat(fileRow[26]));
							mPatientsoleData.setRightPressure6(Float.parseFloat(fileRow[27]));
							mPatientsoleData.setRightPressure7(Float.parseFloat(fileRow[28]));
							mPatientsoleData.setRightPressure8(Float.parseFloat(fileRow[29]));
							mPatientsoleData.setRightPressure9(Float.parseFloat(fileRow[30]));
							mPatientsoleData.setRightPressure10(Float.parseFloat(fileRow[31]));
							mPatientsoleData.setRightPressure11(Float.parseFloat(fileRow[32]));
							mPatientsoleData.setRightPressure12(Float.parseFloat(fileRow[33]));
							mPatientsoleData.setRightAccelerationX(Float.parseFloat(fileRow[34]));
							mPatientsoleData.setRightAccelerationY(Float.parseFloat(fileRow[35]));
							mPatientsoleData.setRightAccelerationZ(Float.parseFloat(fileRow[36]));
							mPatientsoleData.setRightTotalForce(Float.parseFloat(fileRow[37]));
							rtp = rtp + "" + fileRow[37] + ",";
							mPatientsoleData.setRightCopX(Float.parseFloat(fileRow[38]));
							mPatientsoleData.setRightCopY(Float.parseFloat(fileRow[39]));
							mPatientsoleData.setRightTemperature(Float.parseFloat(fileRow[40]));
							mPatientsoleData.setPatientId(pid);
							mPatientsoleData.setFile(inputFile);
							session.save(mPatientsoleData);
						}
					}
					graphdata = ltp + "&" + rtp + "&" + time;
					log.debug("data for graph onload inside declaration " + ltp + "right " + rtp + "time " + time);
					Statistics statistics = findStatistics(pid, inputFile, session, null, null, "upload");

					PatientRestingAndMinOfActivityTime patientRestingAndMoaTime = findAndSaveRestingTimeAndMinOfActivity(
							pid, inputFile, session, timeDifference);

					com.mga.model.PatientSoleDataFiles mPatientsoleDataFiles = new com.mga.model.PatientSoleDataFiles();
					mPatientsoleDataFiles.setPatientId(pid);
					mPatientsoleDataFiles.setSoleDataTxtFile(inputFile);
					// byte[] fileContent = new byte[(int) inputFile.length()];

					Blob blob = Hibernate.getLobCreator(session).createBlob(uploadedInputStream, fileDetail.getSize());
					mPatientsoleDataFiles.setFileContent(blob);

					/*
					 * java.nio.file.Path tempFile =
					 * Files.createTempFile("temp-", ".ext");
					 * Files.copy(uploadedInputStream, tempFile,
					 * StandardCopyOption.REPLACE_EXISTING);
					 * 
					 * fileInputStream = new FileInputStream(tempFile.toFile());
					 * fileInputStream.read(fileContent);
					 * fileInputStream.close();
					 */

					// Files.delete(tempFile);

					Date currentTime = new Date();
					mPatientsoleDataFiles
							.setSoleDataTxtFileInsertTimestamp(new java.sql.Timestamp(currentTime.getTime()));
					mPatientsoleDataFiles.setFileTimestamp(new java.sql.Timestamp(date.getTime()));
					mPatientsoleDataFiles.setNotes(additionalNotes);
					mPatientsoleDataFiles.setMinLeftPressure(statistics.getMinLeftPressure());
					mPatientsoleDataFiles.setMinRightPressure(statistics.getMinRightPressure());
					mPatientsoleDataFiles.setMaxLeftPressure(statistics.getMaxLeftPressure());
					mPatientsoleDataFiles.setMaxRightPressure(statistics.getMaxRightPressure());
					mPatientsoleDataFiles.setMeanLeftPressure(statistics.getMeanLeftPressure());
					mPatientsoleDataFiles.setMeanRightPressure(statistics.getMeanRightPressure());
					mPatientsoleDataFiles.setVarLeftPressure(statistics.getVarLeftPressure());
					mPatientsoleDataFiles.setVarRightPressure(statistics.getVarRightPressure());
					mPatientsoleDataFiles.setStdDevLeftPressure(statistics.getSdLeftPressure());
					mPatientsoleDataFiles.setStdDevRightPressure(statistics.getSdRightPressure());
					mPatientsoleDataFiles.setRestingTimeLeft(patientRestingAndMoaTime.getRestingTimeLeft());
					mPatientsoleDataFiles.setRestingTimeRight(patientRestingAndMoaTime.getRestingTimeRight());
					mPatientsoleDataFiles.setMinOfActivityLeft(patientRestingAndMoaTime.getMinOfActivityLeft());
					mPatientsoleDataFiles.setMinOfActivityRight(patientRestingAndMoaTime.getMinOfActivityRight());

					session.save(mPatientsoleDataFiles);
					transaction.commit();
					blob.free();

					patientAnalysisResult.setFile(inputFile);
					patientAnalysisResult.setStatistics(statistics);
					patientAnalysisResult.setRestingAndMoaTime(patientRestingAndMoaTime);
					patientAnalysisResult.setGraphData(graphdata);
					patientAnalysisResultList.add(patientAnalysisResult);
					patientsAnalysisResult.setPatientAnalysisResult(patientAnalysisResultList);

					log.debug(inputFile + " has been uploaded for patient " + pid + "in DB ");
					log.debug("Total lines updated in DB - " + nrow);
				} catch (HibernateException e) {
					statusCode = 500;
					statusMsg = "Internal Server Error, contact administrator.";
					log.error("Error while uploading patient's sole data - " + e.getMessage());
					e.printStackTrace();
					status.setCode(statusCode);
					status.setMessage(statusMsg);
					patientsAnalysisResult.setStatus(status);

					try {
						transaction.rollback();
					} catch (RuntimeException rbe) {
						log.error("Couldn’t roll back transaction", rbe);

						return patientsAnalysisResult;
					}

					return patientsAnalysisResult;
				} catch (ArrayIndexOutOfBoundsException e) {
					statusCode = 500;
					statusMsg = "Unsupported file is sent...";
					log.error("Error while uploading patient's sole data - " + e.getMessage());
					e.printStackTrace();
					status.setCode(statusCode);
					status.setMessage(statusMsg);
					patientsAnalysisResult.setStatus(status);

					try {
						transaction.rollback();
					} catch (RuntimeException rbe) {
						log.error("Couldn’t roll back transaction", rbe);

						return patientsAnalysisResult;
					}

					return patientsAnalysisResult;
				} catch (Exception e) {
					statusCode = 500;
					statusMsg = "Internal Server Error, contact administrator.";
					log.error("Error while uploading patient's sole data - " + e.getMessage());
					e.printStackTrace();
					status.setCode(statusCode);
					status.setMessage(statusMsg);
					patientsAnalysisResult.setStatus(status);

					try {
						transaction.rollback();
					} catch (RuntimeException rbe) {
						log.error("Couldn’t roll back transaction", rbe);

						return patientsAnalysisResult;
					}

					return patientsAnalysisResult;
				} finally {
					try {
						if (reader != null)
							reader.close();
						if (session != null)
							session.close();
					} catch (IOException e) {
						statusCode = 500;
						statusMsg = "Internal Server Error, contact administrator.";
						log.error("Error while uploading patient's sole data - " + e.getMessage());
						e.printStackTrace();
						status.setCode(statusCode);
						status.setMessage(statusMsg);
						patientsAnalysisResult.setStatus(status);
						return patientsAnalysisResult;
					}
				}
				statusMsg = inputFile + " uploaded to database successfully...";
			}
		}

		status.setCode(statusCode);
		status.setMessage(statusMsg);
		patientsAnalysisResult.setStatus(status);

		return patientsAnalysisResult;
	}

	@POST
	@Path("/deletesoledata")
	public Response deletePatientSoledata(@QueryParam(value = "id") Integer id,
			@QueryParam(value = "patientid") Integer patientId, @QueryParam(value = "file") String file) {

		log.debug("Request received from UI for Patient with ID " + patientId
				+ " to delete the sole data related to file " + file);

		Integer statusCode = 200;
		String statusMsg = null;

		if (id == 0 || id == null || patientId == 0 || patientId == null || file.equals("")) {
			log.debug("Bad request");
			statusCode = 400;
			statusMsg = "Bad Request. Mandatory fields are sent empty or null.";
		} else {
			Session session = null;
			Transaction transaction = null;
			try {
				session = HibernateUtil.getSessionFactory().openSession();
				transaction = session.beginTransaction();
				transaction.setTimeout(5);

				String hql = "DELETE FROM PatientSoleDataFiles WHERE id = :id";
				Query query = session.createQuery(hql);
				query.setParameter("id", id);
				int result = query.executeUpdate();
				log.debug("Records deleted from PatientSoleDataFiles - " + result);

				hql = "DELETE FROM PatientSoleData WHERE patientId = :id AND file = :file";
				query = session.createQuery(hql);
				query.setParameter("id", patientId);
				query.setParameter("file", file);
				result = query.executeUpdate();
				log.debug("Records deleted from PatientSoleData - " + result);

				log.debug("Sole data related to file " + file + " deleted from DB for the patient " + patientId);

				transaction.commit();
			} catch (Exception e) {
				statusCode = 500;
				statusMsg = "Internal Server Error, contact administrator.";
				log.error("Error while deleting the Patient sole data file - " + e.getMessage());
				e.printStackTrace();

				try {
					transaction.rollback();
				} catch (RuntimeException rbe) {
					log.error("Couldn’t roll back transaction", rbe);

					return Response.status(statusCode).entity(statusMsg).build();
				}

				return Response.status(statusCode).entity(statusMsg).build();
			} finally {
				if (session != null) {
					session.close();
				}
			}

			statusMsg = "Requested patient sole data deleted succesfully.";
		}

		return Response.status(statusCode).entity(statusMsg).build();
	}

	/**************************
	 * Fetch files list functions
	 *****************************************/

	@GET
	@Path("/getfiledatalist")
	@Produces({ MediaType.APPLICATION_XML })
	public PatientSoleDataFileList getSoleFileDataList(@QueryParam(value = "id") Integer pid) {
		log.debug("Request to fetch the patients sole data files list is recieved from UI");

		List<SoleDataList> fileDataList = new ArrayList<SoleDataList>();
		PatientSoleDataFileList soledata = new PatientSoleDataFileList();
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction transaction = null;

		Integer statusCode = 200;
		String statusMsg = null;

		if (pid == 0 || pid == null) {
			log.debug("Bad request. Patient ID is sent empty or null.");
			statusCode = 400;
			statusMsg = "Bad Request. Patient ID is sent empty or null.";
		} else {
			log.debug("Request to fetch the patients sole data files list for patient with ID " + pid
					+ " is recieved from UI");
			try {

				transaction = session.beginTransaction();
				String hql = " select id, soleDataTxtFile, fileTimestamp FROM PatientSoleDataFiles WHERE patientId= :pid ORDER BY fileTimestamp";
				Query query = session.createQuery(hql);
				query.setParameter("pid", pid);

				@SuppressWarnings("unchecked")
				List<PatientSoleDataFiles> results = query.list();
				for (Iterator it = results.iterator(); it.hasNext();) {
					com.mga.beans.SoleDataList fsdata = new com.mga.beans.SoleDataList();
					Object[] result = (Object[]) it.next();
					Integer id = (Integer) result[0];
					String fileName = (String) result[1];
					Date fileDate = (Date) result[2];
					SimpleDateFormat requiredFormat = new SimpleDateFormat("MM/dd/yyyy");
					String formattedDate = requiredFormat.format(fileDate);
					log.debug("Found " + " " + fileName + " " + fileDate + " " + formattedDate);
					fsdata.setId(id);
					fsdata.setFilename(fileName);
					// fsdata.setDate(MGAUtil.birthDateDateToString(fileDate));
					fsdata.setDate(formattedDate);
					fileDataList.add(fsdata);

				}
				soledata.setSoleDataList(fileDataList);
				transaction.commit();
				statusMsg = "Success";
				soledata.setCode(statusCode);
				soledata.setStatus(statusMsg);
			} catch (HibernateException e) {
				// transaction.rollback();
				statusCode = 500;
				statusMsg = "Internal Server Error, contact administrator.";
				soledata.setCode(statusCode);
				soledata.setStatus(statusMsg);
				log.error("Error while selecting the Patients sole data files list - " + e.getMessage());
				e.printStackTrace();
				return soledata;
			} finally {
				session.close();
			}
		}

		return soledata;

	}

	@GET
	@Path("/getotherfilelist")
	@Produces({ MediaType.APPLICATION_XML })
	public PatientOtherDataFileList getOtherFileList(@QueryParam(value = "id") Integer pid) {
		log.debug("Request to fetch the patients other files list is recieved from UI");

		List<OtherDataList> fileDataList = new ArrayList<OtherDataList>();
		PatientOtherDataFileList otherData = new PatientOtherDataFileList();
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction transaction = null;

		Integer statusCode = 200;
		String statusMsg = null;
		if (pid == 0 || pid == null) {
			log.debug("Bad request. Patient ID is sent empty or null.");
			statusCode = 400;
			statusMsg = "Bad Request. Patient ID is sent empty or null.";
		} else {
			log.debug("Request to fetch the patients other files list for the patient with ID " + pid
					+ " is recieved from UI");

			try {

				transaction = session.beginTransaction();
				String hql = " select id, fileName, fileInsertTimestamp FROM PatientOtherFiles WHERE patientId= :id ORDER BY fileInsertTimestamp";
				Query query = session.createQuery(hql);
				query.setParameter("id", pid);
				@SuppressWarnings("unchecked")
				List<PatientOtherFiles> results = query.list();
				for (Iterator it = results.iterator(); it.hasNext();) {
					com.mga.beans.OtherDataList fsdata = new com.mga.beans.OtherDataList();
					Object[] result = (Object[]) it.next();
					Integer id = (Integer) result[0];
					String fileName = (String) result[1];
					Date fileDate = (Date) result[2];
					SimpleDateFormat requiredFormat = new SimpleDateFormat("MM/dd/yyyy");
					String formattedDate = requiredFormat.format(fileDate);
					log.debug("Found " + " " + fileName + " " + fileDate);
					fsdata.setId(id);
					fsdata.setFilename(fileName);
					// fsdata.setDate(MGAUtil.birthDateDateToString(fileDate));
					fsdata.setDate(formattedDate);
					fileDataList.add(fsdata);

				}
				otherData.setOtherDataList(fileDataList);
				transaction.commit();
				statusMsg = "Success";
				// otherData.setCode(statusCode);
				// otherData.setStatus(statusMsg);
			} catch (HibernateException e) {
				// transaction.rollback();
				statusCode = 500;
				statusMsg = "Internal Server Error, contact administrator.";
				otherData.setCode(statusCode);
				otherData.setStatus(statusMsg);
				log.error("Error while selecting the Patients sole data files list - " + e.getMessage());
				e.printStackTrace();
				return otherData;
			} finally {
				session.close();
			}
		}

		otherData.setCode(statusCode);
		otherData.setStatus(statusMsg);

		return otherData;

	}

	/**************************
	 * Data analysis functions
	 *****************************************/

	@POST
	@Path("/fetchanalysisresult")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_XML)
	public PatientsAnalysisResult fetchAnalysisResult(
			@FormParam("sole-data-files-analysis-input") SoleDataFilesForAnalysisInput soleDataFilesForAnalysisInput) {

		Integer statusCode = 200;
		String statusMsg = null;

		Integer patientId = soleDataFilesForAnalysisInput.getPatientId();
		Integer numberOfFiles = soleDataFilesForAnalysisInput.getNumberOfFiles();
		String fileIds = soleDataFilesForAnalysisInput.getFileIds();
		float stressThreshold = soleDataFilesForAnalysisInput.getStressThreshold();

		PatientsAnalysisResult patientsAnalysisResult = null;
		Status status = new Status();

		log.debug("Sole data file Ids from UI - " + fileIds);

		try {
			if (numberOfFiles > 1) {
				String[] ids = fileIds.split(",");
				patientsAnalysisResult = fetchAnalysisResult(status, ids, patientId, stressThreshold);
			} else if (numberOfFiles == 1) {
				String[] ids = new String[1];
				ids[0] = fileIds;
				patientsAnalysisResult = fetchAnalysisResult(status, ids, patientId, stressThreshold);
			} else {
				statusCode = 400;
				statusMsg = "Bad Request... File Ids are sent empty or null...";
				status.setCode(statusCode);
				status.setMessage(statusMsg);
				patientsAnalysisResult = new PatientsAnalysisResult();
				patientsAnalysisResult.setStatus(status);

				return patientsAnalysisResult;
			}
			statusMsg = "Sole data analyzed result is displayed in the tiles above.";
		} catch (Exception e) {
			statusCode = 500;
			statusMsg = "Internal Server Error, contact administrator.";
			log.error("Error while analysing the patient sole data - " + e.getMessage());
			e.printStackTrace();
			status.setCode(statusCode);
			status.setMessage(statusMsg);
			patientsAnalysisResult = new PatientsAnalysisResult();
			patientsAnalysisResult.setStatus(status);

			return patientsAnalysisResult;
		}

		status.setCode(statusCode);
		status.setMessage(statusMsg);
		patientsAnalysisResult.setStatus(status);

		return patientsAnalysisResult;
	}

	@GET
	@Path("/fetchfilteredanalysisresult")
	@Produces(MediaType.APPLICATION_XML)
	public PatientsAnalysisResult fetchAnalysisResultForGivenTimePeriod(@QueryParam(value = "id") Integer id,
			@QueryParam(value = "start-date") String startDate, @QueryParam(value = "end-date") String endDate,
			@QueryParam(value = "stress-threshold") float stressThreshold) {

		log.debug("Request recieved from UI to provide the analysis result for the filtered time period");

		Integer statusCode = 200;
		String statusMsg = null;

		PatientsAnalysisResult patientsAnalysisResult = new PatientsAnalysisResult();
		PatientAnalysisResult patientAnalysisResult = new PatientAnalysisResult();
		List<PatientAnalysisResult> patientAnalysisResultList = new ArrayList<PatientAnalysisResult>();
		Status status = new Status();

		patientsAnalysisResult.setPatientId(id);

		MGAUtil mgaUtil = new MGAUtil();

		if (id == 0 || id < 0 || startDate == null || endDate == null) {
			log.debug("Input provided is empty or null");
			statusCode = 400;
			statusMsg = "Bad Request. Patient ID, start date and end date should not be empty or null.";
		} else {
			log.debug("Request recieved from UI to provide the analysis result for the filtered time period "
					+ startDate + " and " + endDate + " for the patient with id " + id);

			Session session = null;
			Transaction transaction = null;

			try {
				DatePeriod datePeriod = new DatePeriod();
				datePeriod.setStartDate(startDate);
				datePeriod.setEndDate(endDate);
				patientsAnalysisResult.setDate(datePeriod);
				Date start = mgaUtil.birthDateStringToDate(startDate, "Insert");
				Date end = mgaUtil.birthDateStringToDate(endDate, "Insert");

				session = HibernateUtil.getSessionFactory().openSession();
				transaction = session.beginTransaction();
				transaction.setTimeout(5);

				String graphData = getSingleGraphResults(id, 0, startDate, endDate, "withtime");

				Statistics statistics = findStatistics(id, null, session, start, end, "analysis");

				PatientStressLevelMinExceeded patientStressLevelMinExceeded = findMinutesStressLevelExceeded(id, null,
						session, stressThreshold, start, end, "filter");

				PatientRestingAndMinOfActivityTime patientRestingAndMoaTime = findRestingTimeAndMinOfActivityForfilteredSoleData(
						id, start, end, session);

				patientAnalysisResult.setStatistics(statistics);
				patientAnalysisResult.setStressLevelExceededTime(patientStressLevelMinExceeded);
				patientAnalysisResult.setRestingAndMoaTime(patientRestingAndMoaTime);
				patientAnalysisResult.setGraphData(graphData);
				patientAnalysisResultList.add(patientAnalysisResult);
				patientsAnalysisResult.setPatientAnalysisResult(patientAnalysisResultList);

				transaction.commit();

			} catch (HibernateException e) {
				// transaction.rollback();
				statusCode = 500;
				statusMsg = "Internal Server Error, contact administrator.";
				log.error("Error while uploading patient's sole data - " + e.getMessage());
				e.printStackTrace();
				status.setCode(statusCode);
				status.setMessage(statusMsg);
				patientsAnalysisResult.setStatus(status);
				return patientsAnalysisResult;
			} catch (Exception e) {
				statusCode = 500;
				statusMsg = "Internal Server Error, contact administrator.";
				log.error("Error while uploading patient's sole data - " + e.getMessage());
				e.printStackTrace();
				status.setCode(statusCode);
				status.setMessage(statusMsg);
				patientsAnalysisResult.setStatus(status);
				return patientsAnalysisResult;
			} finally {
				try {
					if (session != null)
						session.close();
				} catch (Exception e) {
					statusCode = 500;
					statusMsg = "Internal Server Error, contact administrator.";
					log.error("Error while analysis patient's sole data for given time period - " + e.getMessage());
					e.printStackTrace();
					status.setCode(statusCode);
					status.setMessage(statusMsg);
					patientsAnalysisResult.setStatus(status);
					return patientsAnalysisResult;
				}
			}
			statusMsg = "Sole data analyzed result is displayed in the tiles above.";
		}

		status.setCode(statusCode);
		status.setMessage(statusMsg);
		patientsAnalysisResult.setStatus(status);

		return patientsAnalysisResult;

	}

	public PatientsAnalysisResult fetchAnalysisResult(Status status, String[] ids, Integer patientId,
			float stressThreshold) {

		PatientsAnalysisResult patientsAnalysisResult = new PatientsAnalysisResult();
		List<PatientAnalysisResult> patientAnalysisResultList = new ArrayList<PatientAnalysisResult>();
		PatientAnalysisResult patientAnalysisResult = null;

		float minLeftPressure = 0;
		float minRightPressure = 0;
		float maxLeftPressure = 0;
		float maxRightPressure = 0;
		float meanLeftPressure = 0;
		float meanRightPressure = 0;
		float varLeftPressure = 0;
		float varRightPressure = 0;
		float sdLeftPressure = 0;
		float sdRightPressure = 0;
		float restingTimeLeft = 0;
		float restingTimeRight = 0;
		float minOfActivityLeft = 0;
		float minOfActivityRight = 0;
		String fileName = "";
		String uploadDate = null;

		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction transaction = session.beginTransaction();

		for (int i = 0; i < ids.length; i++) {
			Integer id = Integer.parseInt(ids[i]);
			String hql = "select minLeftPressure, minRightPressure, maxLeftPressure, maxRightPressure, meanLeftPressure, meanRightPressure, varLeftPressure, varRightPressure, stdDevLeftPressure, stdDevRightPressure, restingTimeLeft, restingTimeRight, minOfActivityLeft, minOfActivityRight, soleDataTxtFile, fileTimestamp FROM PatientSoleDataFiles WHERE patientId = :patientId AND id = :id";
			Query query = session.createQuery(hql);
			query.setParameter("patientId", patientId);
			query.setParameter("id", id);
			List results = query.list();

			for (Iterator it = results.iterator(); it.hasNext();) {
				Object[] myResult = (Object[]) it.next();
				minLeftPressure = (float) myResult[0];
				minRightPressure = (float) myResult[1];
				maxLeftPressure = (float) myResult[2];
				maxRightPressure = (float) myResult[3];
				meanLeftPressure = (float) myResult[4];
				meanRightPressure = (float) myResult[5];
				varLeftPressure = (float) myResult[6];
				varRightPressure = (float) myResult[7];
				sdLeftPressure = (float) myResult[8];
				sdRightPressure = (float) myResult[9];
				restingTimeLeft = (float) myResult[10];
				restingTimeRight = (float) myResult[11];
				minOfActivityLeft = (float) myResult[12];
				minOfActivityRight = (float) myResult[13];
				fileName = myResult[14].toString();
				Date date = (Date) myResult[15];
				SimpleDateFormat requiredFormat = new SimpleDateFormat("MM/dd/yyyy");
				uploadDate = requiredFormat.format(date);
			}

			if (results.size() == 1) {
				PatientStressLevelMinExceeded patientStressLevelMinExceeded = findMinutesStressLevelExceeded(patientId,
						fileName, session, stressThreshold, null, null, "without_filter");
				patientAnalysisResult = new PatientAnalysisResult();
				patientAnalysisResult.setStressLevelExceededTime(patientStressLevelMinExceeded);
				patientAnalysisResult.setFile(fileName);
				patientAnalysisResult.setDate(uploadDate);
				Statistics statistics = new Statistics();
				statistics.setMinLeftPressure(minLeftPressure);
				statistics.setMinRightPressure(minRightPressure);
				statistics.setMaxLeftPressure(maxLeftPressure);
				statistics.setMaxRightPressure(maxRightPressure);
				statistics.setMeanLeftPressure(meanLeftPressure);
				statistics.setMeanRightPressure(meanRightPressure);
				statistics.setVarLeftPressure(varLeftPressure);
				statistics.setVarRightPressure(varRightPressure);
				statistics.setSdLeftPressure(sdLeftPressure);
				statistics.setSdRightPressure(sdRightPressure);
				patientAnalysisResult.setStatistics(statistics);
				PatientRestingAndMinOfActivityTime patientRestingAndMinOfActivityTime = new PatientRestingAndMinOfActivityTime();
				patientRestingAndMinOfActivityTime.setRestingTimeLeft(restingTimeLeft);
				patientRestingAndMinOfActivityTime.setRestingTimeRight(restingTimeRight);
				patientRestingAndMinOfActivityTime.setMinOfActivityLeft(minOfActivityLeft);
				patientRestingAndMinOfActivityTime.setMinOfActivityRight(minOfActivityRight);
				patientAnalysisResult.setRestingAndMoaTime(patientRestingAndMinOfActivityTime);
				log.debug("Results fetched" + getSingleGraphResults(patientId, id, "", "", "withouttime"));
				patientAnalysisResult.setGraphData(getSingleGraphResults(patientId, id, "", "", "withouttime"));

				patientAnalysisResultList.add(patientAnalysisResult);
			}

		}
		transaction.commit();
		patientsAnalysisResult.setPatientAnalysisResult(patientAnalysisResultList);

		if (session != null) {
			session.close();
		}

		return patientsAnalysisResult;
	}

	public PatientRestingAndMinOfActivityTime findAndSaveRestingTimeAndMinOfActivity(Integer id, String file,
			Session session, Integer timeDifference) {

		log.debug("Request recieved for finding the left and right legs resting time");
		float restingTimeLeft = 0;
		float restingTimeRight = 0;
		float minOfActivityTimeLeft = 0;
		float minOfActivityTimeRight = 0;
		float zero = 0;

		PatientRestingAndMinOfActivityTime patientRestingTime = new PatientRestingAndMinOfActivityTime();

		if (id == 0 || id < 0) {
			log.debug("Bad request. Invalid Patient ID is sent.");
			restingTimeLeft = -1;
			restingTimeRight = -1;
		} else {
			log.debug("Request recieved for finding the left and right legs resting time for the patient with ID - "
					+ id);
			try {
				String hql1 = " select count(time) FROM PatientSoleData  WHERE patientId= :id AND file = :file AND leftTotalForce = :zero";
				String hql2 = " select count(time) FROM PatientSoleData  WHERE patientId= :id AND file = :file AND rightTotalForce = :zero";
				String hql3 = " select count(time) FROM PatientSoleData  WHERE patientId= :id AND file = :file AND leftTotalForce > :zero";
				String hql4 = " select count(time) FROM PatientSoleData  WHERE patientId= :id AND file = :file AND rightTotalForce > :zero";

				Query query1 = session.createQuery(hql1);
				query1.setParameter("id", id);
				query1.setParameter("file", file);
				query1.setParameter("zero", zero);

				Query query2 = session.createQuery(hql2);
				query2.setParameter("id", id);
				query2.setParameter("file", file);
				query2.setParameter("zero", zero);

				Query query3 = session.createQuery(hql3);
				query3.setParameter("id", id);
				query3.setParameter("file", file);
				query3.setParameter("zero", zero);

				Query query4 = session.createQuery(hql4);
				query4.setParameter("id", id);
				query4.setParameter("file", file);
				query4.setParameter("zero", zero);

				@SuppressWarnings("unchecked")
				List restingTimeLeftResult = query1.list();
				List restingTimeRightResult = query2.list();
				List minOfActivityLeftResult = query3.list();
				List minOfActivityRightResult = query4.list();

				float td = (float) (timeDifference / 1000);
				log.debug("td - " + td);
				for (Iterator it = restingTimeLeftResult.iterator(); it.hasNext();) {

					restingTimeLeft = (float) (((long) it.next() * td) / 60.0);
					log.debug("Left resting time - " + restingTimeLeft);
				}
				for (Iterator it = restingTimeRightResult.iterator(); it.hasNext();) {
					restingTimeRight = (float) (((long) it.next() * td) / 60.0);
					log.debug("right resting time - " + restingTimeRight);
				}
				for (Iterator it = minOfActivityLeftResult.iterator(); it.hasNext();) {
					minOfActivityTimeLeft = (float) (((long) it.next() * td) / 60.0);
					log.debug("Left Min of activity - " + minOfActivityTimeLeft);
				}
				for (Iterator it = minOfActivityRightResult.iterator(); it.hasNext();) {
					minOfActivityTimeRight = (float) (((long) it.next() * td) / 60.0);
					log.debug("Right Min of activity - " + minOfActivityTimeRight);
				}

			} catch (Exception e) {
				patientRestingTime.setRestingTimeLeft(-1);
				patientRestingTime.setRestingTimeRight(-1);
				patientRestingTime.setMinOfActivityLeft(-1);
				patientRestingTime.setMinOfActivityRight(-1);
				e.printStackTrace();
				return patientRestingTime;
			}
		}

		patientRestingTime.setRestingTimeLeft(restingTimeLeft);
		patientRestingTime.setRestingTimeRight(restingTimeRight);
		patientRestingTime.setMinOfActivityLeft(minOfActivityTimeLeft);
		patientRestingTime.setMinOfActivityRight(minOfActivityTimeRight);

		return patientRestingTime;
	}

	public PatientRestingAndMinOfActivityTime findRestingTimeAndMinOfActivityForfilteredSoleData(Integer id,
			Date startDate, Date endDate, Session session) {

		log.debug("Request recieved for finding the left and right legs resting time");
		float restingTimeLeft = 0;
		float restingTimeRight = 0;
		float minOfActivityTimeLeft = 0;
		float minOfActivityTimeRight = 0;
		float zero = 0;

		PatientRestingAndMinOfActivityTime patientRestingTime = new PatientRestingAndMinOfActivityTime();
		// Session session = HibernateUtil.getSessionFactory().openSession();
		// Transaction transaction = null;

		Integer statusCode = 200;
		String statusMsg = null;

		if (id == 0 || id < 0) {
			log.debug("Bad request. Invalid Patient ID is sent.");
			statusCode = 400;
			statusMsg = "Bad Request. Invalid Patient ID is sent.";
			restingTimeLeft = -1;
			restingTimeRight = -1;
		} else {
			log.debug("Request recieved for finding the left and right legs resting time for the patient with ID - "
					+ id);
			try {
				String hql1 = " select sum(timeDifference) FROM PatientSoleData  WHERE patientId= :id AND time between :start and :end AND leftTotalForce = :zero";
				String hql2 = " select sum(timeDifference) FROM PatientSoleData  WHERE patientId= :id AND time between :start and :end AND rightTotalForce = :zero";
				String hql3 = " select sum(timeDifference) FROM PatientSoleData  WHERE patientId= :id AND time between :start and :end AND leftTotalForce > :zero";
				String hql4 = " select sum(timeDifference) FROM PatientSoleData  WHERE patientId= :id AND time between :start and :end AND rightTotalForce > :zero";

				Query query1 = session.createQuery(hql1);
				query1.setParameter("id", id);
				query1.setParameter("start", startDate);
				query1.setParameter("end", endDate);
				query1.setParameter("zero", zero);

				Query query2 = session.createQuery(hql2);
				query2.setParameter("id", id);
				query2.setParameter("start", startDate);
				query2.setParameter("end", endDate);
				query2.setParameter("zero", zero);

				Query query3 = session.createQuery(hql3);
				query3.setParameter("id", id);
				query3.setParameter("start", startDate);
				query3.setParameter("end", endDate);
				query3.setParameter("zero", zero);

				Query query4 = session.createQuery(hql4);
				query4.setParameter("id", id);
				query4.setParameter("start", startDate);
				query4.setParameter("end", endDate);
				query4.setParameter("zero", zero);

				@SuppressWarnings("unchecked")
				List restingTimeLeftResult = query1.list();
				List restingTimeRightResult = query2.list();
				List minOfActivityLeftResult = query3.list();
				List minOfActivityRightResult = query4.list();

				// float td = (float) (timeDifference / 1000);
				// log.debug("td - " + td);
				for (Iterator it = restingTimeLeftResult.iterator(); it.hasNext();) {
					restingTimeLeft = (float) (((long) it.next()) / 60.0);
					// restingTimeLeft = (float) (((long) it.next() * td) /
					// 60.0);
					log.debug("Left resting time - " + restingTimeLeft);
				}
				for (Iterator it = restingTimeRightResult.iterator(); it.hasNext();) {
					restingTimeRight = (float) (((long) it.next()) / 60.0);
					// restingTimeRight = (float) (((long) it.next() * td) /
					// 60.0);
					log.debug("right resting time - " + restingTimeRight);
				}
				for (Iterator it = minOfActivityLeftResult.iterator(); it.hasNext();) {
					minOfActivityTimeLeft = (float) (((long) it.next()) / 60.0);
					// minOfActivityTimeLeft = (float) (((long) it.next() * td)
					// / 60.0);
					log.debug("Left Min of activity - " + minOfActivityTimeLeft);
				}
				for (Iterator it = minOfActivityRightResult.iterator(); it.hasNext();) {
					minOfActivityTimeRight = (float) (((long) it.next()) / 60.0);
					// minOfActivityTimeRight = (float) (((long) it.next() * td)
					// / 60.0);
					log.debug("Right Min of activity - " + minOfActivityTimeRight);
				}

				statusMsg = "Success";
			} catch (Exception e) {
				statusCode = 500;
				statusMsg = "Internal Server Error, contact administrator.";
				// patientRestingTime.setCode(statusCode);
				// patientRestingTime.setStatus(statusMsg);
				patientRestingTime.setRestingTimeLeft(-1);
				patientRestingTime.setRestingTimeRight(-1);
				patientRestingTime.setMinOfActivityLeft(-1);
				patientRestingTime.setMinOfActivityRight(-1);
				e.printStackTrace();
				return patientRestingTime;
			}
		}

		// patientRestingTime.setCode(statusCode);
		// patientRestingTime.setStatus(statusMsg);
		patientRestingTime.setRestingTimeLeft(restingTimeLeft);
		patientRestingTime.setRestingTimeRight(restingTimeRight);
		patientRestingTime.setMinOfActivityLeft(minOfActivityTimeLeft);
		patientRestingTime.setMinOfActivityRight(minOfActivityTimeRight);

		return patientRestingTime;
	}

	public Statistics findStatistics(Integer pid, String inputFile, Session session, Date startDate, Date endDate,
			String method) {

		log.debug("Inside finding statistics");

		Statistics statistics = new Statistics();

		float minLeftPressure = 0;
		float minRightPressure = 0;
		float maxLeftPressure = 0;
		float maxRightPressure = 0;
		Double meanLeftPressure = 0.0;
		Double meanRightPressure = 0.0;
		Double sdLeftPressure = 0.0;
		Double sdRightPressure = 0.0;
		Double varLeftPressure = 0.0;
		Double varRightPressure = 0.0;
		String hql = null;
		Query query = null;

		try {
			log.debug("pid - " + pid);
			log.debug("file -" + inputFile);
			if (method.equals("upload")) {
				// Statistics per file
				hql = "select min(leftTotalForce), min(rightTotalForce), max(leftTotalForce), max(rightTotalForce), AVG(leftTotalForce), AVG(rightTotalForce), SQRT((SUM(leftTotalForce*leftTotalForce)/COUNT(leftTotalForce)) - (AVG(leftTotalForce) * AVG(leftTotalForce))), SQRT((SUM(rightTotalForce*rightTotalForce)/COUNT(rightTotalForce)) - (AVG(rightTotalForce) * AVG(rightTotalForce))) FROM PatientSoleData WHERE patientId = :patientId AND file = :file";
				query = session.createQuery(hql);
				query.setParameter("patientId", pid);
				query.setParameter("file", inputFile);
			} else {
				// Statistics between time period
				hql = "select min(leftTotalForce), min(rightTotalForce), max(leftTotalForce), max(rightTotalForce), AVG(leftTotalForce), AVG(rightTotalForce), SQRT((SUM(leftTotalForce*leftTotalForce)/COUNT(leftTotalForce)) - (AVG(leftTotalForce) * AVG(leftTotalForce))), SQRT((SUM(rightTotalForce*rightTotalForce)/COUNT(rightTotalForce)) - (AVG(rightTotalForce) * AVG(rightTotalForce))) FROM PatientSoleData WHERE patientId = :patientId AND time between :start and :end";
				query = session.createQuery(hql);
				query.setParameter("patientId", pid);
				query.setParameter("start", startDate);
				query.setParameter("end", endDate);
			}
			List results = query.list();
			for (Iterator it = results.iterator(); it.hasNext();) {
				Object[] myResult = (Object[]) it.next();
				minLeftPressure = (float) myResult[0];
				minRightPressure = (float) myResult[1];
				maxLeftPressure = (float) myResult[2];
				maxRightPressure = (float) myResult[3];
				meanLeftPressure = (Double) myResult[4];
				meanRightPressure = (Double) myResult[5];
				sdLeftPressure = (Double) myResult[6];
				sdRightPressure = (Double) myResult[7];
			}
			varLeftPressure = sdLeftPressure * sdLeftPressure;
			varRightPressure = sdRightPressure * sdRightPressure;
			statistics.setMinLeftPressure(minLeftPressure);
			statistics.setMinRightPressure(minRightPressure);
			statistics.setMaxLeftPressure(maxLeftPressure);
			statistics.setMaxRightPressure(maxRightPressure);
			statistics.setMeanLeftPressure(meanLeftPressure.floatValue());
			statistics.setMeanRightPressure(meanRightPressure.floatValue());
			statistics.setVarLeftPressure(varLeftPressure.floatValue());
			statistics.setVarRightPressure(varRightPressure.floatValue());
			statistics.setSdLeftPressure(sdLeftPressure.floatValue());
			statistics.setSdRightPressure(sdRightPressure.floatValue());
		} catch (Exception e) {
			e.printStackTrace();
		}

		return statistics;
	}

	public PatientStressLevelMinExceeded findMinutesStressLevelExceeded(Integer id, String file, Session session,
			float stressLevel, Date startDate, Date endDate, String method) {

		log.debug(
				"Request recieved for finding the number of minutes the pressure values exceeded the stress level threshold");
		float minutesExceededLeft = 0;
		float minutesExceededRight = 0;

		PatientStressLevelMinExceeded patientStressLevelMinExceeded = new PatientStressLevelMinExceeded();

		if (id == 0 || id < 0) {
			log.debug("Bad request. Invalid Patient ID is sent.");
			minutesExceededLeft = -1;
			minutesExceededRight = -1;
		} else {
			log.debug(
					"Request for finding the number of minutes the pressure values exceeded the stress level threshold for the patient with ID - "
							+ id);
			try {
				String hqlLeft = null;
				String hqlRight = null;
				Query queryLeft = null;
				Query queryRight = null;
				if (method.equals("filter")) {
					hqlLeft = " select sum(timeDifference) FROM PatientSoleData  WHERE patientId= :id AND time between :start and :end AND leftTotalForce > :stresslevel";
					hqlRight = " select sum(timeDifference) FROM PatientSoleData  WHERE patientId= :id AND time between :start and :end AND rightTotalForce > :stresslevel";
					queryLeft = session.createQuery(hqlLeft);
					queryLeft.setParameter("id", id);
					queryLeft.setParameter("start", startDate);
					queryLeft.setParameter("end", endDate);
					queryLeft.setParameter("stresslevel", stressLevel);
					queryRight = session.createQuery(hqlRight);
					queryRight.setParameter("id", id);
					queryRight.setParameter("start", startDate);
					queryRight.setParameter("end", endDate);
					queryRight.setParameter("stresslevel", stressLevel);
				} else {
					hqlLeft = " select sum(timeDifference) FROM PatientSoleData  WHERE patientId= :id AND file = :file AND leftTotalForce > :stresslevel";
					hqlRight = " select sum(timeDifference) FROM PatientSoleData  WHERE patientId= :id AND file = :file AND rightTotalForce > :stresslevel";
					queryLeft = session.createQuery(hqlLeft);
					queryLeft.setParameter("id", id);
					queryLeft.setParameter("file", file);
					queryLeft.setParameter("stresslevel", stressLevel);
					queryRight = session.createQuery(hqlRight);
					queryRight.setParameter("id", id);
					queryRight.setParameter("file", file);
					queryRight.setParameter("stresslevel", stressLevel);
				}

				@SuppressWarnings("unchecked")
				List resultsLeft = queryLeft.list();
				for (Iterator it = resultsLeft.iterator(); it.hasNext();) {
					minutesExceededLeft = (float) (((long) it.next()) / 60.0);
					log.debug("No of minutes exceeded Left- " + minutesExceededLeft);
				}
				List resultsRight = queryRight.list();
				for (Iterator it = resultsRight.iterator(); it.hasNext();) {
					minutesExceededRight = (float) (((long) it.next()) / 60.0);
					log.debug("No of minutes exceeded Right- " + minutesExceededRight);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		patientStressLevelMinExceeded.setStressThreshold(stressLevel);
		patientStressLevelMinExceeded.setMinutesExceededLeft(minutesExceededLeft);
		patientStressLevelMinExceeded.setMinutesExceededRight(minutesExceededRight);

		return patientStressLevelMinExceeded;
	}

	/**************************
	 * Graph functions
	 *****************************************/

	public String getSingleGraphResults(Integer pid, Integer fid, String startdate, String enddate, String method) {

		log.debug("Inside getSingleGraphResults");

		String filename = "";
		String hql = null;
		Query query = null;
		String leftTotalPressure = "";
		String rightTotalPressure = "";
		String time = "";
		java.sql.Date stdate = null;
		java.sql.Date endate = null;
		String response = "";

		if (pid == 0 || pid == null || fid == null) {
			log.debug("Bad request. ID is sent empty or null.");
		} else {
			log.debug("Request to fetch a file data with id - " + fid);

			Session session = null;

			try {
				log.debug("pid - " + pid);
				log.debug("file -" + fid);
				log.debug("method" + method);

				session = HibernateUtil.getSessionFactory().openSession();

				hql = "select soleDataTxtFile FROM PatientSoleDataFiles WHERE patientId = :patientId AND id = :fid";
				query = session.createQuery(hql);
				query.setParameter("patientId", pid);
				query.setParameter("fid", fid);
				List results = query.list();
				log.debug("Result size -" + results.size());
				for (Iterator it = results.iterator(); it.hasNext();) {
					filename = (String) it.next();
				}
				log.debug("filename to draw graph for " + filename);

				if (method.equals("withouttime")) {
					// Statistics per file
					hql = "select leftTotalForce, rightTotalForce, time FROM PatientSoleData WHERE patientId = :patientId AND file = :filename";
					query = session.createQuery(hql);
					query.setParameter("patientId", pid);
					query.setParameter("filename", filename);
				} else if (method.equals("withtime")) {
					log.debug("startdate" + startdate + "end date" + enddate);
					if (!startdate.equals("") && !enddate.equals("")) {
						stdate = new java.sql.Date(MGAUtil.birthDateStringToDate(startdate, "Insert").getTime());
						log.debug("stdate" + stdate);
						endate = new java.sql.Date(MGAUtil.birthDateStringToDate(enddate, "Insert").getTime());
						log.debug("endate" + endate);
					} else {
						return "date wrong format";
					}
					// Statistics between time period
					hql = "select leftTotalForce, rightTotalForce, time FROM PatientSoleData WHERE patientId = :patientId AND time between :start and :end";
					query = session.createQuery(hql);
					query.setParameter("patientId", pid);
					query.setParameter("start", stdate);
					query.setParameter("end", endate);
				}

				List results1 = query.list();
				log.debug("result1 size -" + results1.size());
				for (Iterator it = results1.iterator(); it.hasNext();) {
					Object[] myResult = (Object[]) it.next();
					leftTotalPressure = leftTotalPressure + "" + (float) myResult[0] + ",";
					rightTotalPressure = rightTotalPressure + "" + (float) myResult[1] + ",";
					Timestamp timeResult = (Timestamp) myResult[2];
					String[] timeArray = timeResult.toString().split(" ");
					// time = time + timeArray[1] + ",";
					time = time + "" + ",";

				}

				response = leftTotalPressure + "&" + rightTotalPressure + "&" + time;
				log.debug("response for graph " + response);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (session != null) {
					session.close();
				}
			}
		}

		return response;
	}

	/**************************
	 * Dicom functions
	 *****************************************/

	@GET
	@Path("/getfile")
	public Response getRequestedFile(@QueryParam(value = "id") Integer id) {

		log.debug("Request to fetch the patients other files list is recieved from UI");

		String fileContent = null;
		File file = null;
		ResponseBuilder response = null;
		Integer statusCode = 200;
		String statusMsg = null;
		Blob blobResponse = null;
		if (id == 0 || id == null) {
			log.debug("Bad request. ID is sent empty or null.");
			statusCode = 400;
			statusMsg = "Bad Request. Mandatory fields are sent empty or null.";
			// return Response.status(statusCode).entity(statusMsg).build();
		} else {
			log.debug("Request to fetch a file with id " + id);

			Session session = null;
			try {
				session = HibernateUtil.getSessionFactory().openSession();

				PatientOtherFiles patientOtherFiles = (PatientOtherFiles) session.get(PatientOtherFiles.class, id);
				Blob blob = patientOtherFiles.getFileContent();
				String filename = patientOtherFiles.getFileName();
				byte[] blobBytes = blob.getBytes(1, (int) blob.length());

				// fileContent = new Base64().encodeToString(blobBytes);
				// String fileContentInStr = new String(blobBytes);
				/*
				 * file = new File(filename); FileOutputStream outputStream =
				 * new FileOutputStream(file); outputStream.write(blobBytes);
				 * outputStream.close();
				 */
				InputStream filestrem = blob.getBinaryStream();
				byte[] resp = dicomToJpg(filestrem);
				fileContent = new Base64().encodeToString(resp);
				blobResponse = blob;
				// response = Response.ok((InputStream) filestrem);
				response = Response.ok((String) fileContent);
				// file.delete();
			} catch (Exception e) {
				statusCode = 500;
				statusMsg = "Internal Server Error, contact administrator.";
				log.error("Error while selecting the dicom file content - " + e.getMessage());
				e.printStackTrace();

				// return Response.status(statusCode).entity(statusMsg).build();
			} finally {
				if (session != null) {
					session.close();
				}
			}
		}

		return response.build();
	}

	public byte[] dicomToJpg(InputStream file) {

		byte[] imageInByte = null;
		Iterator<ImageReader> iterator = ImageIO.getImageReadersByFormatName("DICOM");
		while (iterator.hasNext()) {
			ImageReader imageReader = (ImageReader) iterator.next();
			DicomImageReadParam dicomImageReadParam = (DicomImageReadParam) imageReader.getDefaultReadParam();
			try {
				ImageInputStream iis = ImageIO.createImageInputStream(file);
				imageReader.setInput(iis, false);
				myJpegImage = imageReader.read(0, dicomImageReadParam);

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(myJpegImage, "jpg", baos);
				baos.flush();
				imageInByte = baos.toByteArray();

				iis.close();
				if (myJpegImage == null) {
					log.debug("Could not read image!!");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return imageInByte;
	}

	/**************************
	 * Other files upload and delete functions
	 *****************************************/

	@POST
	@Path("/uploadotherfile")
	@Produces(MediaType.APPLICATION_XML)
	public Response uploadOtherFiles(FormDataMultiPart form) {

		log.debug("Other files upload request recieved from UI");

		FormDataBodyPart filePart = form.getField("other-file");
		FormDataContentDisposition fileDetail = filePart.getFormDataContentDisposition();

		InputStream uploadedInputStream = filePart.getValueAs(InputStream.class);
		FormDataBodyPart idPart = form.getField("other-file-pid");
		Integer id = idPart.getValueAs(Integer.class);

		Integer statusCode = 200;
		String statusMsg = null;
		// String dirName = null;
		// FileInputStream fileInputStream = null;

		if (uploadedInputStream.equals(null) || fileDetail.equals(null)) {
			log.debug("Bad request");
			statusCode = 400;
			statusMsg = "Bad Request... Mandatory fields are sent empty or null...";
		} else if (fileDetail.getFileName().equals("") || fileDetail.getFileName().equals(null)) {
			log.debug("File name is empty or null");
			statusCode = 400;
			statusMsg = "Bad Request... File name should not be empty or null...";
		} else {
			String inputFile = fileDetail.getFileName();
			log.debug("Other files upload request recieved from UI for the patient with id " + id);
			log.debug("Other files File details: \n" + "File name - " + inputFile + "\n" + "File Parameters - "
					+ fileDetail.getParameters());

			Transaction transaction = null;
			Session session = null;
			try {

				session = HibernateUtil.getSessionFactory().openSession();
				transaction = session.beginTransaction();
				transaction.setTimeout(5);

				String[] fileNameType = inputFile.split("\\.");

				PatientOtherFiles patientOtherFiles = new PatientOtherFiles();
				patientOtherFiles.setPatientId(id);
				patientOtherFiles.setFileName(inputFile);
				Blob blob = Hibernate.getLobCreator(session).createBlob(uploadedInputStream, fileDetail.getSize());

				patientOtherFiles.setFileContent(blob);
				patientOtherFiles.setFileType(fileNameType[fileNameType.length - 1]);
				Date currentTime = new Date();
				patientOtherFiles.setFileInsertTimestamp(new java.sql.Timestamp(currentTime.getTime()));

				session.save(patientOtherFiles);
				transaction.commit();
				blob.free();

				log.debug(inputFile + " has been uploaded for patient " + id + " in DB ");
				statusMsg = inputFile + " file uploaded successfully";
			} catch (HibernateException e) {
				statusCode = 500;
				statusMsg = "Internal Server Error, contact administrator.";
				log.error("Error while Patient's additional files - " + e.getMessage());
				e.printStackTrace();

				try {
					transaction.rollback();
				} catch (RuntimeException rbe) {
					log.error("Couldn’t roll back transaction", rbe);

					return Response.status(statusCode).entity(statusMsg).build();
				}

				return Response.status(statusCode).entity(statusMsg).build();
			} catch (Exception e) {
				statusCode = 500;
				statusMsg = "Internal Server Error, contact administrator.";
				log.error("Error while Patient's additional files - " + e.getMessage());
				e.printStackTrace();

				try {
					transaction.rollback();
				} catch (RuntimeException rbe) {
					log.error("Couldn’t roll back transaction", rbe);

					return Response.status(statusCode).entity(statusMsg).build();
				}

				return Response.status(statusCode).entity(statusMsg).build();
			} finally {
				try {
					if (session != null)
						session.close();

				} catch (Exception e) {
					statusCode = 500;
					statusMsg = "Internal Server Error, contact administrator.";
					log.error("Error while Patient's additional files - " + e.getMessage());
					e.printStackTrace();

					try {
						transaction.rollback();
					} catch (RuntimeException rbe) {
						log.error("Couldn’t roll back transaction", rbe);

						return Response.status(statusCode).entity(statusMsg).build();
					}

					return Response.status(statusCode).entity(statusMsg).build();
				}
			}
		}

		return Response.status(statusCode).entity(statusMsg).build();
	}

	@POST
	@Path("/deleteotherfile")
	public Response deletePatientOtherfile(@QueryParam(value = "id") Integer id,
			@QueryParam(value = "patientid") Integer patientId) {

		log.debug(
				"Request received from UI for Patient with ID " + patientId + " to delete the other file with id" + id);

		Integer statusCode = 200;
		String statusMsg = null;

		if (id == 0 || id == null || patientId == 0 || patientId == null) {
			log.debug("Bad request");
			statusCode = 400;
			statusMsg = "Bad Request. Mandatory fields are sent empty or null.";
		} else {
			Session session = null;
			Transaction transaction = null;
			try {
				session = HibernateUtil.getSessionFactory().openSession();
				transaction = session.beginTransaction();
				transaction.setTimeout(5);

				String hql = "DELETE FROM PatientOtherFiles WHERE id = :id";
				Query query = session.createQuery(hql);
				query.setParameter("id", id);
				int result = query.executeUpdate();
				log.debug("Records deleted from PatientSoleDataFiles - " + result);

				transaction.commit();
			} catch (Exception e) {
				statusCode = 500;
				statusMsg = "Internal Server Error, contact administrator.";
				log.error("Error while deleting the Patient other file - " + e.getMessage());
				e.printStackTrace();

				try {
					transaction.rollback();
				} catch (RuntimeException rbe) {
					log.error("Couldn’t roll back transaction", rbe);

					return Response.status(statusCode).entity(statusMsg).build();
				}

				return Response.status(statusCode).entity(statusMsg).build();
			} finally {
				if (session != null) {
					session.close();
				}
			}

			statusMsg = "Requested file deleted succesfully.";
		}

		return Response.status(statusCode).entity(statusMsg).build();
	}

	/**************************
	 * Function scores functions
	 *****************************************/

	@POST
	@Path("/insertfunctionscore")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response insertFunctionScoredata(
			@FormParam("patient-funcdata") PatientsFunctionScoreData patientInFuncScoreData) {

		log.debug("Request for function score insert recieved from UI");

		Integer statusCode = 200;
		String statusMsg = null;

		List<PatientFunctionScoredata> patientInputData = patientInFuncScoreData.getPatientFunctionScore();

		Transaction transaction = null;
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			transaction = session.beginTransaction();
			transaction.setTimeout(5);

			for (Iterator it = patientInputData.iterator(); it.hasNext();) {
				PatientFunctionScoredata patientInputFuncScoreData = (PatientFunctionScoredata) it.next();

				if (patientInputFuncScoreData.getPatientId().equals("")
						|| patientInputFuncScoreData.getPatientId().equals(null)) {
					log.debug("Bad request. Patient ID is sent empty or null.");
					statusCode = 400;
					statusMsg = "Bad Request. Patient ID is sent empty or null.";
				} else if (patientInputFuncScoreData.getFunctionName().equals("")
						|| patientInputFuncScoreData.getFunctionName().equals(null)) {
					log.debug("Bad request. Function name is sent empty or null.");
					statusCode = 400;
					statusMsg = "Bad Request. Function name is sent empty or null.";
				} else if (patientInputFuncScoreData.getFunctionDate().equals("")
						|| patientInputFuncScoreData.getFunctionDate().equals(null)) {
					log.debug("Bad request. Function score Date is sent empty or null.");
					statusCode = 400;
					statusMsg = "Bad Request. Function score Date is sent empty or null.";
				} else {
					log.debug("Request for function score insert for the patient "
							+ patientInputFuncScoreData.getPatientId() + " recieved from UI");

					log.debug("ID - " + patientInputFuncScoreData.getId() + "function score date"
							+ patientInputFuncScoreData.getFunctionDate() + "Patient funcscore - "
							+ patientInputFuncScoreData.getFunctionName() + " ::  value - "
							+ patientInputFuncScoreData.getFunctionValue());

					if (patientInputFuncScoreData.getId() == 0) {
						com.mga.model.PatientFunctionScoreData patientFuncScoreData = new com.mga.model.PatientFunctionScoreData();
						patientFuncScoreData.setPatientId(patientInputFuncScoreData.getPatientId());
						patientFuncScoreData.setFunctionScoreName(patientInputFuncScoreData.getFunctionName());
						patientFuncScoreData.setFunctionScoreValue(patientInputFuncScoreData.getFunctionValue());
						patientFuncScoreData.setFunctionScoreDate(new java.sql.Date(MGAUtil
								.birthDateStringToDate(patientInputFuncScoreData.getFunctionDate(), "FS").getTime()));

						session.save(patientFuncScoreData);
						log.debug(patientInputFuncScoreData.getPatientId() + ", "
								+ patientInputFuncScoreData.getFunctionName()
								+ patientInputFuncScoreData.getFunctionValue()
								+ patientInputFuncScoreData.getFunctionDate() + " function score created in DB ");
						statusMsg = "Function score data created successfully";
					} else {
						String hql = "UPDATE PatientFunctionScoreData set functionScoreName = :functionScoreName, functionScoreValue = :functionScoreValue, "
								+ "functionScoreDate = :functionScoreDate WHERE patientId = :patientId AND id = :id";
						Query query = session.createQuery(hql);
						query.setParameter("functionScoreName", patientInputFuncScoreData.getFunctionName());
						query.setParameter("functionScoreValue", patientInputFuncScoreData.getFunctionValue());
						query.setParameter("functionScoreDate", new java.sql.Date(MGAUtil
								.birthDateStringToDate(patientInputFuncScoreData.getFunctionDate(), "FS").getTime()));
						query.setParameter("patientId", patientInputFuncScoreData.getPatientId());
						query.setParameter("id", patientInputFuncScoreData.getId());
						int result = query.executeUpdate();
						log.debug("Result - " + result);
						log.debug(patientInputFuncScoreData.getPatientId() + ", "
								+ patientInputFuncScoreData.getFunctionName()
								+ patientInputFuncScoreData.getFunctionValue()
								+ patientInputFuncScoreData.getFunctionDate() + " function score updated in DB ");
						statusMsg = "Function score data updated successfully";
					}

				}
			}

			transaction.commit();
		} catch (Exception e) {
			statusCode = 500;
			statusMsg = "Internal Server Error, contact administrator.";
			log.error("Error while inserting the Patient Metadata - " + e.getMessage());
			e.printStackTrace();

			try {
				transaction.rollback();
			} catch (RuntimeException rbe) {
				log.error("Couldn’t roll back transaction", rbe);

				return Response.status(statusCode).entity(statusMsg).build();
			}

			return Response.status(statusCode).entity(statusMsg).build();
		} finally {
			if (session != null) {
				session.close();
			}
		}

		return Response.status(statusCode).entity(statusMsg).build();
	}

	@GET
	@Path("/autoCompletefName")
	@Produces({ MediaType.APPLICATION_XML })
	public String autoCompletefName() {

		log.debug("Request received from UI for autocomplete functionscore names");

		String fsName = "";
		Session session = null;

		try {
			session = HibernateUtil.getSessionFactory().openSession();

			log.debug("rendering function score name and values to UI for autocomplete");
			String hql = "Select distinct functionScoreName FROM PatientFunctionScoreData ORDER BY functionScoreName ";
			Query query = session.createQuery(hql);
			@SuppressWarnings("unchecked")
			List<com.mga.model.PatientFunctionScoreData> results = query.list();
			log.debug("Function Score fetched from DB");
			for (Iterator it = results.iterator(); it.hasNext();) {
				// Object[] result = (Object[]) it.next();
				// String temp = (String) result[1];
				String temp = (String) it.next();
				fsName = fsName + temp;
				fsName = fsName + ",";
			}

		} catch (Exception e) {
			log.error("Error while fetching entire functionscore name  " + e.getMessage());
			e.printStackTrace();
			return "Error while fetching entire functionscore name ";
		} finally {
			if (session != null) {
				session.close();
			}
		}

		return fsName;
	}

	@GET
	@Path("/autoCompletefValue")
	@Produces({ MediaType.APPLICATION_XML })
	public String autoCompletefValue() {

		log.debug("Request received from UI for autocomplete functionscore values");

		String fsValue = "";
		Session session = null;

		try {
			session = HibernateUtil.getSessionFactory().openSession();

			log.debug("rendering function score Name to UI for autocomplete");
			String hql = "Select distinct functionScoreValue FROM PatientFunctionScoreData ORDER BY functionScoreValue ";
			Query query = session.createQuery(hql);
			@SuppressWarnings("unchecked")
			List<com.mga.model.PatientFunctionScoreData> results = query.list();
			log.debug("Function Score fetched from DB");
			for (Iterator it = results.iterator(); it.hasNext();) {
				String temp = (String) it.next();
				fsValue = fsValue + temp;
				fsValue = fsValue + ",";
			}

		} catch (HibernateException e) {
			log.error("Error while fetching entire functionscore Name " + e.getMessage());
			e.printStackTrace();
			return "Error while fetching entire functionscore Name";
		} finally {
			if (session != null) {
				session.close();
			}
		}

		return fsValue;
	}

	@GET
	@Path("/select-functionscore")
	@Produces(MediaType.APPLICATION_XML)
	public PatientsFScore selectFunctionScore(@QueryParam(value = "id") Integer id) {

		log.debug("Request to select the function score recieved from UI");

		List<PatientFScore> fsDataList = new ArrayList<PatientFScore>();
		PatientsFScore patientFScore = new PatientsFScore();

		Integer statusCode = 200;
		String statusMsg = null;

		if (id == 0 || id == null) {
			log.debug("Bad request. Patient ID is sent empty or null.");
			statusCode = 400;
			statusMsg = "Bad Request. Patient ID is sent empty or null.";
		} else {
			log.debug("Request to select the function score for patient " + id + " recieved from UI");

			Session session = null;
			try {
				int count = 0;

				session = HibernateUtil.getSessionFactory().openSession();

				String hql = " select id, functionScoreName, functionScoreValue, functionScoreDate FROM PatientFunctionScoreData  WHERE patient_id= :pid";
				Query query = session.createQuery(hql);
				query.setParameter("pid", id);
				@SuppressWarnings("unchecked")
				List<PatientFunctionScoreData> results = query.list();
				for (Iterator it = results.iterator(); it.hasNext();) {
					com.mga.beans.PatientFScore fsdata = new com.mga.beans.PatientFScore();
					Object[] result = (Object[]) it.next();
					Integer fsId = (Integer) result[0];
					String fsName = (String) result[1];
					String fsvalu = (String) result[2];
					Date fsDate = (Date) result[3];
					count++;
					fsdata.setId(fsId);
					fsdata.setFunctionName(fsName);
					fsdata.setFunctionValue(fsvalu);
					fsdata.setFunctionDate(MGAUtil.birthDateDateToString(fsDate));
					fsDataList.add(fsdata);
				}
				patientFScore.setPatientFScore(fsDataList);
				statusMsg = "Success";
				patientFScore.setCount(count);
			} catch (Exception e) {
				statusCode = 500;
				statusMsg = "Internal Server Error, contact administrator.";
				log.error("Error while selecting the Patients function scores - " + e.getMessage());
				e.printStackTrace();
				patientFScore.setCode(statusCode);
				patientFScore.setStatus(statusMsg);
				return patientFScore;
			} finally {
				if (session != null) {
					session.close();
				}
			}

		}

		patientFScore.setCode(statusCode);
		patientFScore.setStatus(statusMsg);

		return patientFScore;
	}

	@GET
	@Path("/deletefunctionscore")
	public Response deleteFunctionScoredata(@QueryParam(value = "id") Integer id,
			@QueryParam(value = "functionscoreid") String functionScoreId) {

		Integer statusCode = 200;
		String statusMsg = null;

		if (id == 0 || id < 0) {
			log.debug("Bad request");
			statusCode = 400;
			statusMsg = "Bad Request. Mandatory fields are sent empty or null.";
		} else {
			log.debug("Request received from UI for Patient with ID " + id + "function score" + functionScoreId
					+ " Function score delete");

			Session session = HibernateUtil.getSessionFactory().openSession();
			Transaction transaction = null;
			try {
				transaction = session.beginTransaction();
				String hql = "DELETE FROM PatientFunctionScoreData WHERE patientId = :id AND id = :functionScoreId";
				Query query = session.createQuery(hql);
				query.setParameter("functionScoreId", Integer.parseInt(functionScoreId));
				query.setParameter("id", id);
				int result = query.executeUpdate();
				log.debug("Patient with ID " + id + " function score " + functionScoreId + " deleted from DB");
				transaction.commit();
			} catch (HibernateException e) {
				// transaction.rollback();
				statusCode = 500;
				statusMsg = "Internal Server Error, contact administrator.";
				log.error("Error while deleting the Patient function score - " + e.getMessage());
				e.printStackTrace();
				return Response.status(statusCode).entity(statusMsg).build();
			} finally {
				session.close();
			}
		}
		statusMsg = "Requested patient function score deleted succesfully.";
		return Response.status(statusCode).entity(statusMsg).build();

	}

}