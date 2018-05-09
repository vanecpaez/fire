package es.gob.fire.server.admin.service;

import java.io.IOException;
import java.io.StringWriter;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import es.gob.log.consumer.client.LogConsumerClient;
import es.gob.log.consumer.client.ServiceOperations;

/**
 * Servlet implementation class LogAdminService
 */
public class LogAdminService extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = Logger.getLogger(LogAdminService.class.getName());

	private String url = "";//$NON-NLS-1$
	private String nameSrv = "";//$NON-NLS-1$
	private String logFileName = "";//$NON-NLS-1$
	private String opString = "";//$NON-NLS-1$
	private int numlines = 0;
	private String txt2search = "";//$NON-NLS-1$
	private String datetime = "";//$NON-NLS-1$
	private LogConsumerClient logclient = null;
	private  String level = "";//$NON-NLS-1$
	private  long startDateTime = 0L;
	private  long endDateTime = 0L;


	/**
     * @see HttpServlet#HttpServlet()
     */
    public LogAdminService() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		final HttpSession session = request.getSession(false);

		final RequestDispatcher dis = null;
		final ServletContext context = request.getServletContext();

		//final String codeInit  = "I9lUuX+iEvzAD/hwaU2MbQ=="; //$NON-NLS-1$ // I9lUuX+iEvzAD/hwaU2MbQ==
		//D/4avRoIIVNTwjPW4AlhPpXuxCU4Mqdhryj/N6xaFQw=

		this.getParameters(request, session);


		String result = null;

		final String opString = request.getParameter("op"); //$NON-NLS-1$
		if (opString == null) {
			LOGGER.warning("No se ha indicado codigo de operacion"); //$NON-NLS-1$
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No se ha indicado codigo de operacion"); //$NON-NLS-1$
			return;
		}

		ServiceOperations op;
		try {
			op = checkOperation(opString);
		}
		catch (final Exception e) {
			LOGGER.warning(String.format("Codigo de operacion no soportado (%s). Se rechaza la peticion.", opString)); //$NON-NLS-1$
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Codigo de operacion no soportado"); //$NON-NLS-1$
			return;
		}


		if(!op.equals(ServiceOperations.ECHO) && this.logclient == null) {
			LOGGER.warning("No se ha indicado conexion con el servidor de log en sesion"); //$NON-NLS-1$
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No se ha indicado conexion con el servidor de log en sesion"); //$NON-NLS-1$
			return;
		}

		switch (op) {
		case ECHO:
			result = echo(this.url);
			response.getWriter().write(result);
			break;
		case GET_LOG_FILES:
			LOGGER.info("Solicitud entrante de listado de ficheros"); //$NON-NLS-1$
			final byte datLogFiles[] = this.logclient.getLogFiles();
			session.setAttribute("JSON", datLogFiles); //$NON-NLS-1$
			response.sendRedirect(request.getContextPath().toString().concat("/Logs/LogsFileList.jsp?")//$NON-NLS-1$
					.concat(ServiceParams.PARAM_NAMESRV).concat("=").concat(this.nameSrv)); //$NON-NLS-1$

			break;
		case OPEN_FILE:
			LOGGER.info("Solicitud entrante de apertura de fichero"); //$NON-NLS-1$
			result = ""; //$NON-NLS-1$
			final byte datOpenFiles[] = this.logclient.openFile(this.logFileName);

			if(datOpenFiles != null && datOpenFiles.length > 0) {
				session.setAttribute("JSON_LOGINFO", datOpenFiles); //$NON-NLS-1$
			}
			if(this.logclient != null && this.logclient.getCharsetContent()!= null){
				response.setCharacterEncoding(this.logclient.getCharsetContent().toString());
			}

			response.sendRedirect(request.getContextPath().toString().concat("/Logs/LogsManager.jsp?").concat(ServiceParams.PARAM_NAMESRV).concat("=")//$NON-NLS-1$ //$NON-NLS-2$
					 .concat(this.nameSrv).concat("&").concat(ServiceParams.PARAM_FILENAME).concat("=").concat(this.logFileName)  //$NON-NLS-1$//$NON-NLS-2$
					);
			break;
		case CLOSE_FILE:
			LOGGER.info("Solicitud entrante de cierre de fichero"); //$NON-NLS-1$
			//fileClosed = closeFile(req);
			break;
		case TAIL:
			LOGGER.info("Solicitud entrante de consulta del final del log"); //$NON-NLS-1$
			result = ""; //$NON-NLS-1$
			final byte datTailFile[] = this.logclient.getLogTail(this.numlines, this.logFileName);
			if(datTailFile != null && datTailFile.length > 0) {
				final String res = new String(datTailFile,this.logclient.getCharsetContent());
				result += res.replace("\\n", "</br>");//$NON-NLS-1$//$NON-NLS-2$
			}
			if(this.logclient != null && this.logclient.getCharsetContent()!= null){
				response.setCharacterEncoding(this.logclient.getCharsetContent().toString());
			}
			response.getWriter().write(result);
			break;
		case GET_MORE:
			LOGGER.info("Solicitud entrante de mas log"); //$NON-NLS-1$
			result = ""; //$NON-NLS-1$
			final byte datMoreFile[] = this.logclient.getMoreLog(this.numlines);
			if(datMoreFile != null && datMoreFile.length > 0 ) {
				final String res = new String(datMoreFile,this.logclient.getCharsetContent());
				result = res.replace("\\n", "</br>");//$NON-NLS-1$//$NON-NLS-2$
			}
			if(this.logclient != null && this.logclient.getCharsetContent()!= null){
				response.setCharacterEncoding(this.logclient.getCharsetContent().toString());
			}
			response.getWriter().write(result);
			break;
		case SEARCH_TEXT:
			LOGGER.info("Solicitud entrante de busqueda de texto"); //$NON-NLS-1$
			result = ""; //$NON-NLS-1$
			final byte datSearchTxt[] = this.logclient.searchText(this.numlines, this.txt2search, this.datetime);
			if(datSearchTxt != null && datSearchTxt.length > 0 ) {
				final String res = new String(datSearchTxt,this.logclient.getCharsetContent());
				result += res.replace("\\n", "</br>");//$NON-NLS-1$//$NON-NLS-2$
				final String marc = "<span class='highlight'>" + this.txt2search + "</span>"; //$NON-NLS-1$ //$NON-NLS-2$
				result = result.replace(this.txt2search, marc);
			}
			if(this.logclient != null && this.logclient.getCharsetContent()!= null){
				response.setCharacterEncoding(this.logclient.getCharsetContent().toString());
			}
			response.getWriter().write(result);
			break;
		case FILTER:
			LOGGER.info("Solicitud entrante de filtrado de log"); //$NON-NLS-1$
			result = ""; //$NON-NLS-1$

			final byte datFiltered[] = this.logclient.getLogFiltered(this.numlines, this.getStartDateTime(), this.getEndDateTime(), this.getLevel());
			if(datFiltered != null && datFiltered.length > 0 ) {
				final String res = new String(datFiltered,this.logclient.getCharsetContent());
				result += res.replace("\\n", "</br>");//$NON-NLS-1$//$NON-NLS-2$
			}
			if(this.logclient != null && this.logclient.getCharsetContent()!= null){
				response.setCharacterEncoding(this.logclient.getCharsetContent().toString());
			}
			response.getWriter().write(result);
			break;
		case DOWNLOAD:
			LOGGER.info("Solicitud entrante de descarga de fichero"); //$NON-NLS-1$
			//result = download(req);
			break;
		default:
			LOGGER.warning("Operacion no soportada. Este resultado refleja un problema en el codigo del servicio"); //$NON-NLS-1$
			//resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Operacion no soportada sin login previo"); //$NON-NLS-1$
			return;
		}
//		if(dis != null) {
//			dis.forward(request, response);
//		}

//		response.getWriter().write(result);



	}

	/**
	 *
	 * @param url
	 * @return
	 */
	protected final String echo(final String url) {
		final LogConsumerClient lclient = new LogConsumerClient();
		final StringWriter resultEcho = new StringWriter();
		final JsonObjectBuilder jsonObj = Json.createObjectBuilder();
		final JsonArrayBuilder data = Json.createArrayBuilder();
		String result = ""; //$NON-NLS-1$
		try {
			result = lclient.echo(url);
			data.add(Json.createObjectBuilder()
					//.add("Code",response.getStatus()) //$NON-NLS-1$
					.add("Message",result)); //$NON-NLS-1$
			jsonObj.add("Ok", data); //$NON-NLS-1$
			final JsonWriter jw = Json.createWriter(resultEcho);
	        jw.writeObject(jsonObj.build());
	        jw.close();
	        result = resultEcho.toString();
		}
		catch (final IOException e) {
			data.add(Json.createObjectBuilder()
					//.add("Code",response.getStatus()) //$NON-NLS-1$
					.add("Message", e.getMessage())); //$NON-NLS-1$
			jsonObj.add("Error", data); //$NON-NLS-1$
			final JsonWriter jw = Json.createWriter(resultEcho);
	        jw.writeObject(jsonObj.build());
	        jw.close();
	        result = resultEcho.toString();
		}

		return result;

	}



	private static ServiceOperations checkOperation(final String opString)
			throws NumberFormatException, UnsupportedOperationException {

		final int op = Integer.parseInt(opString);
		return ServiceOperations.parseOperation(op);
	}


	/**
	 * Obtiene los par&aacute;metros
	 * @param request
	 * @param session
	 */
	private final void getParameters(final HttpServletRequest request, final HttpSession session) {

		if(request.getParameter(ServiceParams.PARAM_URL) != null && !"".equals(request.getParameter(ServiceParams.PARAM_URL))) { //$NON-NLS-1$
			this.url = request.getParameter(ServiceParams.PARAM_URL);
		}
		if(request.getParameter(ServiceParams.PARAM_NAMESRV) != null && !"".equals(request.getParameter(ServiceParams.PARAM_NAMESRV))) { //$NON-NLS-1$
			this.nameSrv = request.getParameter(ServiceParams.PARAM_NAMESRV);
		}
		if(request.getParameter(ServiceParams.PARAM_FILENAME) != null && !"".equals(request.getParameter(ServiceParams.PARAM_FILENAME))) { //$NON-NLS-1$
			this.logFileName = request.getParameter(ServiceParams.PARAM_FILENAME);
		}

		if(session.getAttribute("LOG_CLIENT")!=null) {
			this.logclient = (LogConsumerClient) session.getAttribute("LOG_CLIENT"); //$NON-NLS-1$
		}
		if(request.getParameter(ServiceParams.PARAM_NLINES) != null && !"".equals(request.getParameter(ServiceParams.PARAM_NLINES))) { //$NON-NLS-1$
			this.numlines = Integer.parseInt(request.getParameter(ServiceParams.PARAM_NLINES));
		}

		if(request.getParameter(ServiceParams.PARAM_TXT2SEARCH) != null && !"".equals(request.getParameter(ServiceParams.PARAM_TXT2SEARCH))) { //$NON-NLS-1$
			this.txt2search = request.getParameter(ServiceParams.PARAM_TXT2SEARCH);
		}
		if(request.getParameter(ServiceParams.PARAM_SEARCHDATE) != null && !"".equals(request.getParameter(ServiceParams.PARAM_SEARCHDATE))) { //$NON-NLS-1$
			this.datetime = request.getParameter(ServiceParams.PARAM_SEARCHDATE);
		}
		if(request.getParameter(ServiceParams.START_DATETIME) != null && !"".equals(request.getParameter(ServiceParams.START_DATETIME))) { //$NON-NLS-1$
			this.setStartDateTime(Long.parseLong(request.getParameter(ServiceParams.START_DATETIME)));
		}
		if(request.getParameter(ServiceParams.END_DATETIME) != null && !"".equals(request.getParameter(ServiceParams.END_DATETIME))) { //$NON-NLS-1$
			this.setEndDateTime(Long.parseLong(request.getParameter(ServiceParams.END_DATETIME)));
		}
		if(request.getParameter(ServiceParams.LEVEL) != null && !"".equals(request.getParameter(ServiceParams.LEVEL))) { //$NON-NLS-1$
			this.setLevel(request.getParameter(ServiceParams.LEVEL));
		}
	}


	/* Propiedades */
	private final String getUrl() {
		return this.url;
	}

	private final void setUrl(final String url) {
		this.url = url;
	}

	private final String getNameSrv() {
		return this.nameSrv;
	}

	private final void setNameSrv(final String nameSrv) {
		this.nameSrv = nameSrv;
	}

	private final String getLogFileName() {
		return this.logFileName;
	}

	private final void setLogFileName(final String logFileName) {
		this.logFileName = logFileName;
	}

	private final String getOpString() {
		return this.opString;
	}

	private final void setOpString(final String opString) {
		this.opString = opString;
	}

	private final String getTxt2search() {
		return this.txt2search;
	}

	private final String getDatetime() {
		return this.datetime;
	}
	 private final String getLevel() {
			return this.level;
	}

	private final void setLevel(final String level) {
			this.level = level;
	}

	private final long getEndDateTime() {
		return this.endDateTime;
	}

	private final void setEndDateTime(final long endDateTime) {
		this.endDateTime = endDateTime;
	}

	private final long getStartDateTime() {
			return this.startDateTime;
	}

	private final void setStartDateTime(final long startDateTime) {
			this.startDateTime = startDateTime;
	}



}
