import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Scanner;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class HttpsOpen {
	
	public static ArrayList<Poliza> listaPolizas;
	public static ArrayList<Recibo> listaRecibos;
	public static DatosMeses datosMeses;
	public static ArrayList<RecibosMes> contabilizacionesMes= new ArrayList<RecibosMes>();
	public static boolean remoto=true;
	public static String rutaFicheros="C:\\Datos\\Proyectos\\Openbank Comisiones Seguros\\Ficheros PROD\\";
	public static ArrayList<String>listaCambioNombre= new ArrayList<String>();
	public static void escribirPolizas(String rutaFichero) {
	  FileWriter fichero = null;
      PrintWriter pw = null;
      try 
      {
          fichero = new FileWriter(rutaFichero);
          pw = new PrintWriter(fichero);
          String cabecera="fichero;idPoliza;suplemento;ramo";
          pw.println(cabecera);
          for (int i = 0; i < listaPolizas.size(); i++) {
        	    	  String linea=listaPolizas.get(i).getNombreFichero()+";";
        	    	  linea+=listaPolizas.get(i).getIdPoliza()+";";
        			  linea+=listaPolizas.get(i).getIdSuplemento()+";";
        			  linea+=listaPolizas.get(i).getRamo()+";";
        			  linea+=listaPolizas.get(i).getIban();
        			  pw.println(linea);
          }
              

      } catch (Exception e) {
          e.printStackTrace();
      } finally {
         try {
         // Nuevamente aprovechamos el finally para 
         // asegurarnos que se cierra el fichero.
         if (null != fichero)
            fichero.close();
         } catch (Exception e2) {
            e2.printStackTrace();
         }
      }
	}
	
	public static Date sumaDia(Date fecha) {
		
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(fecha);
	//	calendar.add(Calendar.DAY_OF_MONTH,1); JMLO NO QUITAR
		return calendar.getTime();


	}
	
	public static Recibo calcularImportesMeses(Recibo recibo) {
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	 
		
		
		if(recibo.getSituacionRecibo()!=null && recibo.getSituacionRecibo().equalsIgnoreCase("CO")) {
		
		Date fechaInicial=null;
		Date fechaFinal=null;
		try {
			fechaInicial = dateFormat.parse(recibo.getFechaEfectoInicial());
			fechaFinal=dateFormat.parse(recibo.getFechaVencimiento());
			 
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
 
		int dias=(int) ((fechaFinal.getTime()-fechaInicial.getTime())/86400000);
		double importeDia=recibo.getPrimaNeta()/dias;
		
		int i=0;
		int diasTotales=0;
	
		while(i<datosMeses.listaFechas.size() && datosMeses.listaFechas.get(i).before(fechaFinal)) {
			DatoMensual datoMes= new DatoMensual();
			if(datosMeses.listaFechas.get(i).after(fechaInicial) && datosMeses.listaFechas.get(i).before(fechaFinal)){
				
				int diasPeriodo= (int) ((datosMeses.listaFechas.get(i).getTime()-fechaInicial.getTime())/86400000);
				diasTotales+=diasPeriodo;
				double importeMes=importeDia*diasPeriodo;
				datoMes.setImporteMes(importeMes);
				datoMes.setFechaMes(datosMeses.listaFechas.get(i));
				recibo.getDatoMeses().add(datoMes);
			}else {
				
				datoMes.setImporteMes(new Double("0.0"));
				datoMes.setFechaMes(datosMeses.listaFechas.get(i));
				recibo.getDatoMeses().add(datoMes);
			}				
			if(fechaInicial.before(datosMeses.listaFechas.get(i)))
				fechaInicial=sumaDia(datosMeses.listaFechas.get(i));			
			i++;
		}
		if(i<datosMeses.listaFechas.size()) {
			if(datosMeses.listaFechas.get(i).after(fechaFinal)&& fechaInicial.before(fechaFinal)) {
				DatoMensual datoMes= new DatoMensual();
				int diasPeriodo= (int) ((fechaFinal.getTime()-fechaInicial.getTime())/86400000)+1;
				diasTotales+=diasPeriodo;
				double importeMes=importeDia*diasPeriodo;
				datoMes.setImporteMes(importeMes);
				datoMes.setFechaMes(fechaFinal);
				recibo.getDatoMeses().add(datoMes);
			}
		}
		
	return recibo;	
		}
		else {
			
			for(int i=0; i<datosMeses.listaFechas.size();i++) {
				DatoMensual datoMes= new DatoMensual();
				datoMes.setImporteMes(new Double("0.0"));
				datoMes.setFechaMes(datosMeses.listaFechas.get(i));
				recibo.getDatoMeses().add(datoMes);
			}
			return recibo;
		}
			
		
	}
	public static String obtenerLineaImportes(Recibo recibo) {
		
		String linea="";
		
		
		SimpleDateFormat objSDF = new SimpleDateFormat("yyyy-MM-dd"); 

		
		boolean escrito=false;
		
		DecimalFormat formato1 = new DecimalFormat("#,000000000000");
		for(int i=0;i<recibo.getDatoMeses().size();i++) {
			String fecha= objSDF.format(recibo.getDatoMeses().get(i).getFechaMes());
			int anioFechaMes=Integer.valueOf(fecha.substring(0,4));
			int mesFechaMes=Integer.valueOf(fecha.substring(5,7));
			int anioVencimiento=Integer.valueOf(recibo.getFechaVencimiento().substring(0,4));
			int mesVencimiento=Integer.valueOf(recibo.getFechaVencimiento().substring(5,7));
			
			if(anioVencimiento>anioFechaMes && !escrito) {
				String temp=";"+recibo.getDatoMeses().get(i).importeMes;
				linea=linea+temp.replace(".", ",");//.replaceAll(".", ",");//JMLO formato1.format(recibo.getDatoMeses().get(i).importeMes);
			}
			else
				if(anioVencimiento==anioFechaMes && mesVencimiento>=mesFechaMes) {
			
				escrito=true;
				String temp=";"+recibo.getDatoMeses().get(i).importeMes;
					linea=linea+temp.replace(".", ",");//.replaceAll(".", ",");//JMLO formato1.format(recibo.getDatoMeses().get(i).importeMes);
				}
				else {
					linea=linea+";0,00000000000000";
				}
		}
		
		return linea;
	}
	
public static String obtenerCabeceraMeses() {
		
		String linea="";
		
		for(int i=0;i<datosMeses.listaFechas.size();i++) {
			
			SimpleDateFormat sf = new SimpleDateFormat("dd-MM-yyyy");
			linea=linea+";"+sf.format(datosMeses.listaFechas.get(i));	
		}
		return linea;
	}

	public static void escribirRecibos(String rutaFichero) {
		  FileWriter fichero = null;
	      PrintWriter pw = null;
	      try
	      {
	          fichero = new FileWriter(rutaFichero);
	          pw = new PrintWriter(fichero);
	          String cabecera="fichero;idPoliza;idRecibo;claseRecibo;suplemento;ramo;fechaEfectoActual;fechaEfectoInicial;fechaEmision;fechaSituacion;fechaVencimiento;situacionRecibo;primeNeta;tienePoliza;validez;fechaQuitado;estadoSustituido;incongruencia;diasTotales;importeDias;importeHastaHoy";
	          cabecera=cabecera+";"+obtenerCabeceraMeses();
	                 
	          pw.println(cabecera);
	          for (int i = 0; i < listaRecibos.size(); i++) {
	        	  System.out.println(listaRecibos.size()+" / "+i);
	        	  String linea=listaRecibos.get(i).getNombreFichero()+";";
	        			  linea+=listaRecibos.get(i).getIdPoliza()+";";
	        			  linea+=listaRecibos.get(i).getIdRecibo()+";";
	        			  linea+=listaRecibos.get(i).getClaseRecibo()+";";
	        			  linea+=listaRecibos.get(i).getNumeroSuplemento()+";";
	        			  linea+=listaRecibos.get(i).getRamo()+";";
	        			  if(listaRecibos.get(i).getFechaEfectoActual().contains("T"))
	        			  linea+=listaRecibos.get(i).getFechaEfectoActual().substring(0,10)+";";
	        			  else
	        				  linea+=listaRecibos.get(i).getFechaEfectoActual()+";";
	        			  if(listaRecibos.get(i).getFechaEfectoInicial().contains("T"))
		        			  linea+=listaRecibos.get(i).getFechaEfectoInicial().substring(0,10)+";";
		        			  else
		        				  linea+=listaRecibos.get(i).getFechaEfectoInicial()+";";
	        			  if(listaRecibos.get(i).getFechaEmision().contains("T"))
		        			  linea+=listaRecibos.get(i).getFechaEmision().substring(0,10)+";";
		        			  else
		        				  linea+=listaRecibos.get(i).getFechaEmision()+";";
	        			  
	        			  if(listaRecibos.get(i).getFechaSituacion().contains("T"))
		        			  linea+=listaRecibos.get(i).getFechaSituacion().substring(0,10)+";";
		        			  else
		        				  linea+=listaRecibos.get(i).getFechaSituacion()+";";
	        			  if(listaRecibos.get(i).getFechaVencimiento().contains("T"))
		        			  linea+=listaRecibos.get(i).getFechaVencimiento().substring(0,10)+";";
		        			  else
		        				  linea+=listaRecibos.get(i).getFechaVencimiento()+";";
	
	        			  
		        	      linea+=listaRecibos.get(i).getSituacionRecibo()+";";
	        			 
	        			  linea+=listaRecibos.get(i).getPrimaNeta().toString()+";";//.replace(".",",")+";";
	        			
	        			  linea+=listaRecibos.get(i).isTienePoliza()+";";
	        			  
	        			  
	        			  linea+=listaRecibos.get(i).isValido()+";";
	        			if(listaRecibos.get(i).getFechaQuitado()==null)
	        				 linea+=""+";";
	        			else
	        			  linea+=listaRecibos.get(i).getFechaQuitado()+";";
	        			if(listaRecibos.get(i).getEstadoReciboSustituto()==null)
	        				 linea+=""+";";
	        			else
	        			  linea+=listaRecibos.get(i).getEstadoReciboSustituto()+";";
	        			if(listaRecibos.get(i).getIncongruente()==null)
	        				 linea+=""+";";
	        			else
	        			  linea+=listaRecibos.get(i).getIncongruente()+";";
	        			 String fechaInicial=listaRecibos.get(i).getFechaEfectoActual();
	        			  Date dateInicial=new SimpleDateFormat("yyyy-MM-dd").parse(fechaInicial);  
	        			 String fechaVencimiento=listaRecibos.get(i).getFechaVencimiento();
	        			  Date dateFinal=new SimpleDateFormat("yyyy-MM-dd").parse(fechaVencimiento);  
	                      long difference = dateFinal.getTime() - dateInicial.getTime();
	                      float daysBetween = (difference / (1000*60*60*24));
	                      int diasTotales = Math.round(daysBetween);
	                      linea+=diasTotales+";";
	                     Double importeDiasStr= new Double(listaRecibos.get(i).getPrimaNeta()/diasTotales);
	                      linea+=importeDiasStr.toString().replace(".", ",")+";";
	                      Double importePorDia=new Double(listaRecibos.get(i).getPrimaNeta()/diasTotales);
	                   
	                      listaRecibos.get(i).setImportePorDia(importePorDia);
	                      int dias=diasContabilizados(listaRecibos.get(i));
	                      if(dias==0)
	                    	  linea+="0,00;";
	                      else {
	                    	 Double numero = new Double( (listaRecibos.get(i).getPrimaNeta()/diasTotales)*dias);
	                    	 String texto= String.valueOf(numero).replace(".", ",");
	                    	 linea+=texto+";";
	                      }
	                    
	         
	        			  linea+=obtenerLineaImportes(listaRecibos.get(i));
	        			  pw.println(linea);
	          }
	              

	      } catch (Exception e) {
	          e.printStackTrace();
	      } finally {
	         try {
	         // Nuevamente aprovechamos el finally para 
	         // asegurarnos que se cierra el fichero.
	         if (null != fichero)
	            fichero.close();
	         } catch (Exception e2) {
	            e2.printStackTrace();
	         }
	      }
	      generarEstadisticaMensual("d:\\estadisticasMes.csv");
		}
	
public static void generarEstadisticaMensual(String rutaFichero)	{


FileWriter fichero = null;
PrintWriter pw = null;
DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");  

    try {
		fichero = new FileWriter(rutaFichero);
		pw = new PrintWriter(fichero);
		String cabecera="Mes;ImportePeriodificado;ImporteTotalMes";         
		pw.println(cabecera);
		for(int i=0;i<datosMeses.listaFechas.size();i++) {
			EstadisticaMes datoMes=obtenerDatoMes(datosMeses.listaFechas.get(i));
			String strDate = dateFormat.format(datosMeses.listaFechas.get(i));  
			String linea=strDate+";"+String.valueOf(datoMes.getImportePeridificado())//.replace(".", ",")
			+";"+String.valueOf(datoMes.getImporteTotal());//.replace(".", ",");
			pw.println(linea);
			
		}
		 if (null != fichero)
	            fichero.close();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
   
}

public static EstadisticaMes obtenerDatoMes(Date dateInicial) {
	
	EstadisticaMes resultado= new EstadisticaMes();
	 try {
		
	 
	for(int i=0; i<listaRecibos.size();i++) {
		
		
		if(listaRecibos.get(i).isValido() && listaRecibos.get(i).getSituacionRecibo().equalsIgnoreCase("CO")) {
			Date fechaSituacion=new SimpleDateFormat("yyyy-MM-dd").parse(listaRecibos.get(i).getFechaSituacion());	
			Date fechaInicio=new SimpleDateFormat("yyyy-MM-dd").parse(listaRecibos.get(i).getFechaEfectoActual());
			Date fechaFinal=new SimpleDateFormat("yyyy-MM-dd").parse(listaRecibos.get(i).getFechaVencimiento());
				if(fechaSituacion.getYear()==dateInicial.getYear() &&fechaSituacion.getMonth()==dateInicial.getMonth()) {

					Double importePrima=new Double(listaRecibos.get(i).getPrimaNeta());
					Double sumaImporte= resultado.getImporteTotal()+importePrima;
					resultado.setImporteTotal(sumaImporte);
				}
				if(dateInicial.getTime()>=fechaInicio.getTime() &&dateInicial.getTime()<=fechaFinal.getTime()) {
					
                    if(listaRecibos.get(i).getIdRecibo().contains("000040025540"))
                  	  System.out.println("dentros");
					
					DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");  
					String strDate = dateFormat.format(dateInicial);  
					
					
					String anioInicial=strDate.substring(0,4);
					String mesInicial=strDate.substring(5,7);
					String diaInicial="01";
				
					Date fechaDiaUno=new SimpleDateFormat("yyyy-MM-dd").parse(anioInicial+"-"+mesInicial+"-"+diaInicial);
					
					Date calculoInicial=new Date();
					Date calculoFinal=new Date();
					if(fechaInicio.getTime()>=fechaDiaUno.getTime())
						calculoInicial=fechaInicio;
					else
						calculoInicial=fechaDiaUno;
					
					if(fechaFinal.getTime()>=dateInicial.getTime())
						calculoFinal=dateInicial;
					else
						calculoFinal=fechaFinal;
					
					long difference = calculoFinal.getTime() -calculoInicial.getTime();
                    float daysBetween = (difference / (1000*60*60*24));
                    int diasTotales = Math.round(daysBetween)+2;
                    Double importePeriodificado=diasTotales*listaRecibos.get(i).getImportePorDia();
                    Double sumaImportePeriodifica= resultado.getImportePeridificado()+importePeriodificado;
					resultado.setImportePeridificado(sumaImportePeriodifica);
				}
				
		}
		
	}
	
	 } catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	
	return resultado;
	
}
public static int diasContabilizados(Recibo recibo) {
	 String fechaInicial=recibo.getFechaEfectoActual();
	  Date dateInicial;
	try {
		dateInicial = new SimpleDateFormat("yyyy-MM-dd").parse(fechaInicial);
		String fechaVencimiento=recibo.getFechaVencimiento();
		Date dateFinal=new SimpleDateFormat("yyyy-MM-dd").parse(fechaVencimiento);  
	    long difference = 0;
	    Date fechaHoy= new Date();
	    if(fechaHoy.getTime()>dateFinal.getTime())
	    	difference=dateFinal.getTime() - dateInicial.getTime();
	    else{
	    	difference = fechaHoy.getTime() - dateInicial.getTime();
	    }
	   if(difference<0)
		   difference=0;
	    float daysBetween = (difference / (1000*60*60*24));
	     int diasTotales = Math.round(daysBetween);
	     
	     return diasTotales;
	} catch (ParseException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		return 0;
	}  
   
	
}
public static ArrayList<String> obtenerListadoFicheros(String ruta){
	
	ArrayList<String> ficheros= new ArrayList<String>();
	File carpeta = new File(ruta);
	String[] listado = carpeta.list();
	if (listado == null || listado.length == 0) {
	    System.out.println("No hay elementos dentro de la carpeta actual");
	    return ficheros;
	}
	else {
	    for (int i=0; i< listado.length; i++) {
	        if(listado[i].endsWith(".XML")||listado[i].endsWith(".xml"))
	        	ficheros.add(listado[i]);
	    }
	}
	return ficheros;
}
		
public static ArrayList<String> obtenerListadoFicheros(){
	 URL urlForGetRequest;
		try {
			
			  TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
		           
					@Override
					public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
						// TODO Auto-generated method stub
						
					}

					@Override
					public void checkClientTrusted(X509Certificate[] chain, String authType)
							throws CertificateException {
						// TODO Auto-generated method stub
						
					}

					@Override
					public X509Certificate[] getAcceptedIssuers() {
						// TODO Auto-generated method stub
						return null;
					}
		        }
		        };

		        // Install the all-trusting trust manager
		        SSLContext sc = SSLContext.getInstance("SSL");
		        sc.init(null, trustAllCerts, new java.security.SecureRandom());
		        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

		        // Create all-trusting host name verifier
		        HostnameVerifier allHostsValid = new HostnameVerifier() {
		            
					@Override
					public boolean verify(String arg0, SSLSession arg1) {
						// TODO Auto-generated method stub
						return true;
					}
		        };

		        // Install the all-trusting host verifier
		      HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
			
		//	urlForGetRequest = new URL("https://concomseg-sftp.openbank.gs.corp/sftp/lista/in");
			urlForGetRequest = new URL("https://agps-comcomseg-sftp-es.prod.ok-cloud.net/sftp/lista/in");
		//	urlForGetRequest = new URL("https://agps-comcomseg-sftp-es.dev.ok-cloud.net/db/collections?query=select%20*%20from%20seguros_comis_contab");
		     String readLine = null;

		     HttpsURLConnection conection = (HttpsURLConnection) urlForGetRequest.openConnection();
		   
		     conection.setRequestMethod("GET");

		 //    conection.setRequestProperty("userId", "a1bcdef"); // set userId its a sample here

		     int responseCode = conection.getResponseCode();

		     if (responseCode == HttpURLConnection.HTTP_OK) {
                   BufferedReader in = new BufferedReader(
		              new InputStreamReader(conection.getInputStream()));
		             StringBuffer response = new StringBuffer();

		         while ((readLine = in .readLine()) != null) {

		             response.append(readLine);

		         } in .close();

		         System.out.println("JSON String Result " + response.toString());
		         String filtrado=response.substring(response.indexOf("[")+1,response.indexOf("]"));
		        String listaFicheros[]= filtrado.split(",");
		        ArrayList listadoRet= new ArrayList<String>();
		        for(int i=0;i<listaFicheros.length;i++) {
//if(listaFicheros[i].contains("-20240425")) {
		        	if(listaFicheros[i].contains("2Transferred_")) {
		        		listaCambioNombre.add(listaFicheros[i].replaceAll("\"",""));
		        		listadoRet.add(listaFicheros[i].replaceAll("\"", "").trim().replaceAll("2Transferred_", ""));
		        	}else 
		        	if(listaFicheros[i].contains("EIAC-ENV-E0189-0000099879-20240402055526-003"))
		        		System.out.println("Encontrado");
		        	listadoRet.add(listaFicheros[i].replaceAll("\"", "").trim());
	//}		        	
		        }
		        Collections.sort(listadoRet);
		        return listadoRet;
		     } else {
		    	 System.out.println("GET NOT WORKED");
		    	 return null;
		     	}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return null;
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			return null;
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			return null;
		}
		 
}
public static String  existeFicheroEnRenombrados(String nombre) {
	
for(int i=0;i<listaCambioNombre.size();i++) {
	if(listaCambioNombre.get(i).contains(nombre)) return listaCambioNombre.get(i);
}
return nombre;
}


public static boolean existeFichero(String nombre) {
	 FileWriter fichero = null;
    PrintWriter pw = null;
    
   nombre =existeFicheroEnRenombrados(nombre);

   
   	 File archivo = new File(rutaFicheros+nombre);
   	 if(archivo.exists()) {
   		 System.out.println("No descargamos porque existe fichero "+nombre);
   		 return true;
   	 }
   	 else 
   		 return false;
   			
}
public static void escribirFichero(String nombre,StringBuffer datos) {
	 FileWriter fichero = null;
     PrintWriter pw = null;
     try
     {
    	 File archivo = new File("C:\\Datos\\Proyectos\\Openbank Comisiones Seguros\\Ficheros PROD\\"+nombre);
    	 if(archivo.exists())
    		 System.out.println("No descargamos el fichero "+nombre+" porque existe");
    	 else {    		 
         fichero = new FileWriter("C:\\Datos\\Proyectos\\Openbank Comisiones Seguros\\Ficheros PROD\\"+nombre);
         pw = new PrintWriter(fichero);
             pw.println(datos);
             System.out.println("------------->Descargamos el fichero "+nombre+" Nuevo");
    	 }
     } catch (Exception e) {
         e.printStackTrace();
     } finally {
        try {
        // Nuevamente aprovechamos el finally para 
        // asegurarnos que se cierra el fichero.
        	  if (null != fichero)
                  fichero.close();
               } catch (Exception e2) {
                  e2.printStackTrace();
               }
     }
}
public static void analizaPoliza(String xmlPolizas,String nombreFichero) {
	
	DocumentBuilderFactory factory =
			DocumentBuilderFactory.newInstance();
			DocumentBuilder builder=null;
			Document doc =null;
			try {
				builder = factory.newDocumentBuilder();
				doc = builder.parse(new InputSource(new StringReader(xmlPolizas)));
				Element root = doc.getDocumentElement();
			NodeList listaNodos=root.getChildNodes();
			NodeList polizas=null;
			for(int i=0;i<listaNodos.getLength();i++) {
				if(listaNodos.item(i).getNodeName().equalsIgnoreCase("Objetos")) {
					polizas=listaNodos.item(i).getChildNodes();
					break;
				}
			}
			for(int i=0;i<polizas.getLength();i++) {
					if(polizas.item(i).getNodeName().equalsIgnoreCase("Poliza")) {
						NodeList listaDatos=polizas.item(i).getChildNodes();
						Poliza poliza= new Poliza();		
						for(int j=0;j<listaDatos.getLength();j++) {
							if(listaDatos.item(j).getNodeName().equalsIgnoreCase("DatosPoliza")){					
								poliza=obtenerDatosPoliza(listaDatos.item(j).getChildNodes());					
				}
							if(listaDatos.item(j).getNodeName().equalsIgnoreCase("GestionCobro")){					
								String iban=obtenerDatosPolizaGestionCobro(listaDatos.item(j).getChildNodes());
								poliza.setIban(iban);
				}
		 }
		 poliza.setNombreFichero(existeFicheroEnRenombrados(nombreFichero));
		 listaPolizas.add(poliza);	
		}
							
			}
		
			} catch (ParserConfigurationException | SAXException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
	
}

public static void sustituirRecibo(Recibo recibo) {

	boolean borrado=false;
	
	if(recibo.getIdRecibo().contains("4614363570"))
			System.out.println("Analizo "+recibo.getSituacionRecibo());
	if(recibo.getSituacionRecibo()!=null) {
	for(int i=0; i<listaRecibos.size();i++) {
		
		if(listaRecibos.get(i).getSituacionRecibo()==null) {
			listaRecibos.get(i).setEstadoReciboSustituto(recibo.getFechaEfectoInicial());
			listaRecibos.get(i).setValido(false);
			listaRecibos.get(i).setFechaQuitado(recibo.getFechaEmision());
			listaRecibos.get(i).setEstadoReciboSustituto(recibo.getSituacionRecibo());
		}else if(listaRecibos.get(i).isValido()){
		if(listaRecibos.get(i).getNumeroSuplemento()!=null) {
		if(listaRecibos.get(i).getIdRecibo().equalsIgnoreCase(recibo.getIdRecibo())
				&& listaRecibos.get(i).getNumeroSuplemento().equalsIgnoreCase(recibo.getNumeroSuplemento())
				&& listaRecibos.get(i).getIdPoliza().equalsIgnoreCase(recibo.getIdPoliza())
				) {
			if(recibo.getSituacionRecibo().equalsIgnoreCase("AN")||(recibo.getSituacionRecibo().equalsIgnoreCase("DE"))) {
				if(listaRecibos.get(i).getSituacionRecibo().equalsIgnoreCase("DE")&&recibo.getSituacionRecibo().equalsIgnoreCase("AN"))	
					listaRecibos.get(i).setIncongruente("CORRECTO ERA DE Y PASO A AN");
				else
					if(listaRecibos.get(i).getSituacionRecibo().equalsIgnoreCase("DE")&&recibo.getSituacionRecibo().equalsIgnoreCase("DE"))	
						listaRecibos.get(i).setIncongruente("CORRECTO REPETIDO DE");
					else
						if(listaRecibos.get(i).getSituacionRecibo().equalsIgnoreCase("AN")&&recibo.getSituacionRecibo().equalsIgnoreCase("DE"))	
							listaRecibos.get(i).setIncongruente("INCORRECTO DE AN PASA A DE");
						else
							if(listaRecibos.get(i).getSituacionRecibo().equalsIgnoreCase("AN")&&recibo.getSituacionRecibo().equalsIgnoreCase("AN"))	
								listaRecibos.get(i).setIncongruente("CORRECTO REPETIDO AN");
				
							else
								if(listaRecibos.get(i).getSituacionRecibo().equalsIgnoreCase("PE")&&recibo.getSituacionRecibo().equalsIgnoreCase("AN")) {	
									listaRecibos.get(i).setIncongruente("CORRECTO DE PE PASA A AN");
								}
								else
									if(listaRecibos.get(i).getSituacionRecibo().equalsIgnoreCase("PE")&&recibo.getSituacionRecibo().equalsIgnoreCase("DE")) {	
										listaRecibos.get(i).setIncongruente("CORRECTO DE PE PASA A DE");
									}
									else
										if(listaRecibos.get(i).getSituacionRecibo().equalsIgnoreCase("CO")&&recibo.getSituacionRecibo().equalsIgnoreCase("DE")) {	
											listaRecibos.get(i).setIncongruente("CORRECTO DE CO PASA A DE");
											borrado=true;
										}
										else
											if(listaRecibos.get(i).getSituacionRecibo().equalsIgnoreCase("CO")&&recibo.getSituacionRecibo().equalsIgnoreCase("AN")) {	
												listaRecibos.get(i).setIncongruente("CORRECTO DE CO PASA A AN");
												borrado=true;
											} 
				
				listaRecibos.get(i).setEstadoReciboSustituto(recibo.getFechaEfectoInicial());
				listaRecibos.get(i).setValido(false);
				listaRecibos.get(i).setFechaQuitado(recibo.getFechaEmision());
				listaRecibos.get(i).setEstadoReciboSustituto(recibo.getSituacionRecibo());
				
				if(borrado) {
					
					try {
						String fecha =recibo.getNombreFichero();
						fecha=fecha.substring(26,34);
						Date fechaEmision=new SimpleDateFormat("yyyyMMdd").parse(fecha);
						int punto=0;
						for(int j=0;j<listaRecibos.get(i).getDatoMeses().size();j++) {
							if(fechaEmision.getYear()==listaRecibos.get(i).getDatoMeses().get(j).getFechaMes().getYear() &&
							fechaEmision.getMonth()==listaRecibos.get(i).getDatoMeses().get(j).getFechaMes().getMonth()) {
								punto=j;
								break;
							}
						}
						for(int j=punto;j<listaRecibos.get(i).getDatoMeses().size();j++) {
							listaRecibos.get(i).getDatoMeses().get(j).importeMes=new Double("0.0");
						}
						borrado=false;
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
			}
		
		if(recibo.getSituacionRecibo().equalsIgnoreCase("CO")) {
			if(listaRecibos.get(i).getSituacionRecibo().equalsIgnoreCase("PE"))	
				listaRecibos.get(i).setIncongruente("CORRECTO ERA PE Y PASO A CO");
			else 
				if(listaRecibos.get(i).getSituacionRecibo().equalsIgnoreCase("CO"))	
					listaRecibos.get(i).setIncongruente("ERROR ERA CO y PASO A CO");
				else 
					if(listaRecibos.get(i).getSituacionRecibo().equalsIgnoreCase("AN"))	
						listaRecibos.get(i).setIncongruente("ERROR ERA AN y PASO A CO");
					else 
						if(listaRecibos.get(i).getSituacionRecibo().equalsIgnoreCase("DE"))	
							listaRecibos.get(i).setIncongruente("ERROR ERA DE y PASO A CO");
			
			listaRecibos.get(i).setEstadoReciboSustituto(recibo.getFechaEfectoInicial());
			listaRecibos.get(i).setValido(false);
			listaRecibos.get(i).setFechaQuitado(recibo.getFechaEmision());
			listaRecibos.get(i).setEstadoReciboSustituto(recibo.getSituacionRecibo());
		}
		
		if(recibo.getSituacionRecibo().equalsIgnoreCase("PE")) {
			if(listaRecibos.get(i).getSituacionRecibo().equalsIgnoreCase("PE"))	{
				listaRecibos.get(i).setIncongruente("CORRECTO ERA PE Y PASO A PE");
				listaRecibos.get(i).setValido(false);
			}
			else 
				if(listaRecibos.get(i).getSituacionRecibo().equalsIgnoreCase("CO"))	{
					listaRecibos.get(i).setIncongruente("ERROR ERA CO y PASO A PE");
					listaRecibos.get(i).setValido(true);
				}
				else 
					if(listaRecibos.get(i).getSituacionRecibo().equalsIgnoreCase("AN"))	{
						listaRecibos.get(i).setIncongruente("ERROR ERA AN y PASO A PE");
						listaRecibos.get(i).setValido(true);
					}
					else 
						if(listaRecibos.get(i).getSituacionRecibo().equalsIgnoreCase("DE")) {	
							listaRecibos.get(i).setIncongruente("ERROR ERA DE y PASO A PE");
							listaRecibos.get(i).setValido(true);
						}
			
			listaRecibos.get(i).setEstadoReciboSustituto(recibo.getFechaEfectoInicial());			
			listaRecibos.get(i).setFechaQuitado(recibo.getFechaEmision());
			listaRecibos.get(i).setEstadoReciboSustituto(recibo.getSituacionRecibo());
		}
		}
		}
	}
	}
	}
	
}


public static void analizaRecibo(String xmlPolizas,String nombreFichero) {
	
	DocumentBuilderFactory factory =
			DocumentBuilderFactory.newInstance();
			DocumentBuilder builder=null;
			Document doc =null;
			try {
				builder = factory.newDocumentBuilder();
				doc = builder.parse(new InputSource(new StringReader(xmlPolizas)));
				Element root = doc.getDocumentElement();
			NodeList listaNodos=root.getChildNodes();
			NodeList recibos=null;
			for(int i=0;i<listaNodos.getLength();i++) {
				if(listaNodos.item(i).getNodeName().equalsIgnoreCase("Objetos")) {
					recibos=listaNodos.item(i).getChildNodes();
					break;
				}
			}
			for(int i=0;i<recibos.getLength();i++) {
					if(recibos.item(i).getNodeName().equalsIgnoreCase("Recibo")) {
						NodeList listaDatos=recibos.item(i).getChildNodes();
						Recibo recibo= new Recibo();	
						Poliza poliza= new Poliza();
						for(int j=0;j<listaDatos.getLength();j++) {
							if(listaDatos.item(j).getNodeName().equalsIgnoreCase("DatosPoliza")){					
								poliza=obtenerDatosPoliza(listaDatos.item(j).getChildNodes());					
							}
							else if(listaDatos.item(j).getNodeName().equalsIgnoreCase("DatosRecibo")){					
								recibo=obtenerDatosRecibo(listaDatos.item(j).getChildNodes());					
							}
		 }
		 recibo.setNombreFichero(existeFicheroEnRenombrados(nombreFichero));
		 if( !(recibo.getFechaSituacion().length() < 10))
		 recibo.setDatoContabilizacion(recibo.getFechaSituacion().subSequence(0, 7).toString().replace("-", ""));
		 recibo.getFechaSituacion();
		 recibo.setIdPoliza(poliza.getIdPoliza());
		 recibo.setNumeroSuplemento(poliza.getIdSuplemento());
		 recibo.setRamo(poliza.getRamo());
		 sustituirRecibo(recibo);
		 recibo=calcularImportesMeses(recibo);
		
		 listaRecibos.add(recibo);	
		}
							
			}
		
			} catch (ParserConfigurationException | SAXException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
	
}




public static Poliza obtenerDatosPoliza(NodeList datosPoliza) {
	Poliza poliza= new Poliza();	
	for(int i=0;i<datosPoliza.getLength();i++) {
		if(datosPoliza.item(i).getNodeName().equalsIgnoreCase("IdPoliza"))
			poliza.setIdPoliza(datosPoliza.item(i).getChildNodes().item(0).getNodeValue());
		if(datosPoliza.item(i).getNodeName().equalsIgnoreCase("NumeroSuplemento"))
			poliza.setIdSuplemento(datosPoliza.item(i).getChildNodes().item(0).getNodeValue());
		else if(datosPoliza.item(i).getNodeName().equalsIgnoreCase("DatosRamo")) {
			NodeList listaTemp=datosPoliza.item(i).getChildNodes();
			for(int j=0;j<listaTemp.getLength();j++) {
				if(listaTemp.item(j).getNodeName().equalsIgnoreCase("ModalidadRamo")) {
					poliza.setRamo(listaTemp.item(j).getChildNodes().item(0).getNodeValue());
				}
			}
		}
	}
	
	return poliza;
}


public static String obtenerDatosPolizaGestionCobro(NodeList datosPoliza) {
	String retorno="";
	Poliza poliza= new Poliza();	
	for(int i=0;i<datosPoliza.getLength();i++) {
		if(datosPoliza.item(i).getNodeName().equalsIgnoreCase("DatosFormaPago")) {
			NodeList listaTemp=datosPoliza.item(i).getChildNodes();
			for(int j=0;j<listaTemp.getLength();j++) {
				if(listaTemp.item(j).getNodeName().equalsIgnoreCase("DatosCuentaCorriente")) {
					NodeList listaTemp2=listaTemp.item(j).getChildNodes();
					for(int k=0;k<listaTemp2.getLength();k++) {
						
						if(listaTemp2.item(k).getNodeName().equalsIgnoreCase("IBAN")) {
						retorno=listaTemp2.item(k).getChildNodes().item(0).getNodeValue();
						break;
						}	
					}
				}
			}
		}
	}
	
	return retorno;
}

public static StringBuffer leerFicheroCompleto(String ruta) {
	  File archivo = null;
      FileReader fr = null;
      BufferedReader br = null;
      StringBuffer response = new StringBuffer();
      try {
         // Apertura del fichero y creacion de BufferedReader para poder
         // hacer una lectura comoda (disponer del metodo readLine()).
         archivo = new File (ruta);
         fr = new FileReader (archivo);
         br = new BufferedReader(fr);

         // Lectura del fichero
         String linea;
         while((linea=br.readLine())!=null)
        	 response.append(linea);
         
         return response;
      }
     
      catch(Exception e){
         e.printStackTrace();
      }finally{
         // En el finally cerramos el fichero, para asegurarnos
         // que se cierra tanto si todo va bien como si salta 
         // una excepcion.
         try{                    
            if( null != fr ){   
               fr.close();     
            }                  
         }catch (Exception e2){ 
            e2.printStackTrace();
         }
      }
      
return null;
}


public static Recibo obtenerDatosRecibo(NodeList datosRecibo) {
	Recibo recibo= new Recibo();	
	for(int i=0;i<datosRecibo.getLength();i++) {
		if(datosRecibo.item(i).getNodeName().equalsIgnoreCase("IdRecibo"))
			recibo.setIdRecibo(datosRecibo.item(i).getChildNodes().item(0).getNodeValue());
		else if(datosRecibo.item(i).getNodeName().equalsIgnoreCase("SituacionRecibo"))
			recibo.setSituacionRecibo(datosRecibo.item(i).getChildNodes().item(0).getNodeValue());
		else if(datosRecibo.item(i).getNodeName().equalsIgnoreCase("ClaseRecibo"))
			recibo.setClaseRecibo(datosRecibo.item(i).getChildNodes().item(0).getNodeValue());
		else if(datosRecibo.item(i).getNodeName().equalsIgnoreCase("Fechas")) {
			NodeList listaTemp=datosRecibo.item(i).getChildNodes();
			for(int j=0;j<listaTemp.getLength();j++) {
				if(listaTemp.item(j).getNodeName().equalsIgnoreCase("FechaSituacion")) {
					recibo.setFechaSituacion(listaTemp.item(j).getChildNodes().item(0).getNodeValue());
				}
				if(listaTemp.item(j).getNodeName().equalsIgnoreCase("FechaEfectoInicial")) {
					recibo.setFechaEfectoInicial(listaTemp.item(j).getChildNodes().item(0).getNodeValue());
				}
				if(listaTemp.item(j).getNodeName().equalsIgnoreCase("FechaEfectoActual")) {
					recibo.setFechaEfectoActual(listaTemp.item(j).getChildNodes().item(0).getNodeValue());
				}
				if(listaTemp.item(j).getNodeName().equalsIgnoreCase("FechaVencimiento")) {
					recibo.setFechaVencimiento(listaTemp.item(j).getChildNodes().item(0).getNodeValue());
				}
				if(listaTemp.item(j).getNodeName().equalsIgnoreCase("FechaEmision")) {
					recibo.setFechaEmision(listaTemp.item(j).getChildNodes().item(0).getNodeValue());
				}
			}
		}
		
		else if(datosRecibo.item(i).getNodeName().equalsIgnoreCase("DatosComisiones")) {
			NodeList listaTempAnterior=datosRecibo.item(i).getChildNodes();
			NodeList listaTemp=null;
			for(int j=0;j<listaTempAnterior.getLength();j++) {
				if(listaTempAnterior.item(j).getNodeName().equalsIgnoreCase("Comision")) {
					listaTemp=listaTempAnterior.item(j).getChildNodes();
					break;
				}
			}
			for(int j=0;j<listaTemp.getLength();j++) {
				if(listaTemp.item(j).getNodeName().equalsIgnoreCase("ComisionLiquida")) {
					
					double primaNeta= new Double(Double.valueOf(listaTemp.item(j).getChildNodes().item(0).getNodeValue()));//.replace(".",",")));
					recibo.setPrimaNeta(primaNeta);
				}
			
			}
		}
	}
	
	return recibo;
}



public static void recuperarFicheros(ArrayList<String>listaFicheros) {
    HostnameVerifier allHostsValid = new HostnameVerifier() {
        
		@Override
		public boolean verify(String arg0, SSLSession arg1) {
			// TODO Auto-generated method stub
			return true;
		}
    };

    // Install the all-trusting host verifier
  HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);



  URL urlForGetRequest;
  ValidadorXml validador= new ValidadorXml();
try {
	
for(int i=0;i<listaFicheros.size();i++) {	
	
	if(listaFicheros.get(i).contains("EIAC-ENV-E0189-0000099879-20240402055526-003"))
		System.out.println("Estamos con listaFichero");
	
if(!existeFichero(listaFicheros.get(i))) {	
	
	String nombre= existeFicheroEnRenombrados(listaFicheros.get(i));

//urlForGetRequest = new URL("https://concomseg-sftp-opb-comisiones-seguros-pro.appls.boae.paas.gsnetcloud.corp/sftp/fichero/?ruta=/comisioneseguros/areaintercambio/entrada/"+listaFicheros.get(i));
	urlForGetRequest = new URL("https://agps-comcomseg-sftp-es.prod.ok-cloud.net/sftp/fichero/?ruta=/comisioneseguros/areaintercambio/entrada/"+nombre);

//	urlForGetRequest = new URL("https://agps-comcomseg-sftp-es.prod.ok-cloud.net/sftp/fichero/?ruta=/comisioneseguros/areaintercambio/ficheros-contables/"+listaFicheros.get(i));

 String readLine = null;

 HttpsURLConnection conection = (HttpsURLConnection) urlForGetRequest.openConnection();

 conection.setRequestMethod("GET");

 int responseCode = conection.getResponseCode();

 if (responseCode == HttpURLConnection.HTTP_OK) {
       BufferedReader in = new BufferedReader(
          new InputStreamReader(conection.getInputStream()));
         StringBuffer response = new StringBuffer();

     while ((readLine = in .readLine()) != null) {

         response.append(readLine+"\n");

     } in .close();
     boolean valido =true;
   
 if(response.toString().contains("<Recibo>")) {
	 System.out.println("Analizando fichero:"+nombre);
	 analizaRecibo(response.toString(),nombre);
       valido =validador.validateXMLSchema(".//src/resources/xsd/contabilidad-zurich-recibo.xsd", response.toString());
 }else
	 if(response.toString().contains("<Poliza>")) {

		 analizaPoliza(response.toString(),nombre);//response.toString().substring(39,response.toString().length()));
	 }
 
//if (valido)
	escribirFichero(listaFicheros.get(i),response);
}
}
else {
	String readLine = null;
	StringBuffer response = new StringBuffer();
	String  nombre =existeFicheroEnRenombrados(listaFicheros.get(i));
    File initialFile = new File(rutaFicheros+nombre);
    InputStream targetStream = new FileInputStream(initialFile);
    BufferedReader in = new BufferedReader(new InputStreamReader(targetStream));

	     while ((readLine = in .readLine()) != null) {

	         response.append(readLine+"\n");

	     } in .close();
	     boolean valido =true;
	   
	 if(response.toString().contains("<Recibo>")) {
		 System.out.println("Analizando fichero:"+listaFicheros.get(i));
		 analizaRecibo(response.toString(),listaFicheros.get(i));
	       valido =validador.validateXMLSchema(".//src/resources/xsd/contabilidad-zurich-recibo.xsd", response.toString());
	 }else
		 if(response.toString().contains("<Poliza>")) {

			 analizaPoliza(response.toString(),listaFicheros.get(i));//response.toString().substring(39,response.toString().length()));
		 }
	 
}
	
	
/*if(listaFicheros.get(i).equalsIgnoreCase("EIAC-ENV-E0189-0000099879-20211212064048-003.XML")||listaFicheros.get(i).equalsIgnoreCase("EIAC-ENV-E0189-0000099879-20211212060500-002.XML")) {
	
StringBuffer response=leerFicheroCompleto("C:\\Datos\\Proyectos\\Openbank Comisiones Seguros\\OPENBANK INICIALES 12-12-2021\\"+listaFicheros.get(i));	

if(response.toString().contains("<Recibo>")) {
	 System.out.println("Analizando fichero:"+listaFicheros.get(i));
	 analizaRecibo(response.toString(),listaFicheros.get(i));
	 boolean valido =true;
      valido =validador.validateXMLSchema(".//src/resources/xsd/contabilidad-zurich-recibo.xsd", response.toString());
}else
	 if(response.toString().contains("<Poliza>")) {

		 analizaPoliza(response.toString(),listaFicheros.get(i));//response.toString().substring(39,response.toString().length()));
	 }

//if (valido)
	escribirFichero(listaFicheros.get(i),response);

}*/
	
}


} catch (MalformedURLException e) {
	// TODO Auto-generated catch block
	e.printStackTrace();
} catch (IOException e) {
	// TODO Auto-generated catch block
	e.printStackTrace();
}

}




public static void recuperarFicherosDaily(String nombreFichero) {
    HostnameVerifier allHostsValid = new HostnameVerifier() {
        
		@Override
		public boolean verify(String arg0, SSLSession arg1) {
			// TODO Auto-generated method stub
			return true;
		}
    };

    // Install the all-trusting host verifier
  HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);



  URL urlForGetRequest;
  ValidadorXml validador= new ValidadorXml();

	
	
//urlForGetRequest = new URL("https://concomseg-sftp-opb-comisiones-seguros-pro.appls.boae.paas.gsnetcloud.corp/sftp/fichero/?ruta=/comisioneseguros/areaintercambio/entrada/"+listaFicheros.get(i));
	try {
		urlForGetRequest = new URL("https://agps-comcomseg-sftp-es.prod.ok-cloud.net/sftp/fichero/?ruta=/comisioneseguros/areaintercambio/ficheros-contables/"+nombreFichero);
	

 String readLine = null;

 HttpsURLConnection conection = null;
try {
	conection = (HttpsURLConnection) urlForGetRequest.openConnection();
} catch (Exception e) {
	// TODO Auto-generated catch block
	e.printStackTrace();
}

 try {
	conection.setRequestMethod("GET");
} catch (Exception e) {
	// TODO Auto-generated catch block
	e.printStackTrace();
}

 int responseCode = 0;
try {
	responseCode = conection.getResponseCode();
} catch (Exception e) {
	// TODO Auto-generated catch block
	e.printStackTrace();
}

 if (responseCode == HttpURLConnection.HTTP_OK) {
       BufferedReader in = null;
	try {
		in = new BufferedReader(
		      new InputStreamReader(conection.getInputStream()));
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
         StringBuffer response = new StringBuffer();

     try {
		while ((readLine = in .readLine()) != null) {

		     response.append(readLine+"\n");

		 }
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} try {
		in .close();
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
     boolean valido =true;
  
	escribirFichero(nombreFichero,response);
 }
	} catch ( IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}


public static void recuperarFicherosDailyLocal(String nombreFichero) {
   
  ValidadorXml validador= new ValidadorXml();
  String readLine = null;
       BufferedReader in = null;
	try {
	    File initialFile = new File(rutaFicheros+nombreFichero);
	    InputStream targetStream = new FileInputStream(initialFile);
		in = new BufferedReader(
		      new InputStreamReader(targetStream));
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
         StringBuffer response = new StringBuffer();

     try {
		while ((readLine = in .readLine()) != null) {

		     response.append(readLine+"\n");

		 }
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} try {
		in .close();
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}

}




public static void generarEstadisticaMes() {
	

	
	SimpleDateFormat d = new SimpleDateFormat("yyyyMM");
	
	for(int i=0; i<datosMeses.listaFechas.size();i++) {
		
		String dato=d.format(datosMeses.listaFechas.get(i));		
		RecibosMes ReciboMesTemp=new RecibosMes();
		ReciboMesTemp.setMesContable(dato);		
		contabilizacionesMes.add(ReciboMesTemp);
			
	}
	
	for(int i=0; i<listaRecibos.size();i++) {
		dividirPorMes(listaRecibos.get(i));
		
	}
	
	System.out.println();
	
}

public static int daysBetween(Date d1, Date d2){
    return (int)( (d2.getTime() - d1.getTime()) / (1000 * 60 * 60 * 24));
}

public static Double calcularImporteDia(String fechaInicio, String fechaFin,Double importePrima) {
	
	SimpleDateFormat objSDF = new SimpleDateFormat("yyyy-MM-dd"); 
	
	try {
		Date dateInicio=objSDF.parse(fechaInicio);
		Date dateFin=objSDF.parse(fechaFin);
		
		int dias=daysBetween(dateInicio,dateFin)+2;
		Double importePorDia=new Double(0.00);
		if(dias==0)
			System.out.println(dateInicio+" "+dateFin);
		else
		 importePorDia=importePrima/dias;
		return importePorDia;
	} catch (ParseException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		return new Double(0.00);
	}
}

public static void dividirPorMes(Recibo recibo) {
	
	
	for(int i=0;i<contabilizacionesMes.size();i++) {
		
		if(recibo.getDatoContabilizacion().equalsIgnoreCase(contabilizacionesMes.get(i).getMesContable())) {
			Double importePorDia=calcularImporteDia(recibo.getFechaEfectoInicial(),recibo.getFechaVencimiento(),recibo.getPrimaNeta());
			recibo.setImportePorDia(importePorDia);
			contabilizacionesMes.get(i).getRecibosMes().add(recibo);
			break;
		}
	}
	
}



public static void rellenarSiPoliza(){
	
	for(int i=0;i<listaRecibos.size();i++) {
		String idPoliza=listaRecibos.get(i).getIdPoliza();
		if(idPoliza!=null) {
		for(int j=0;j<listaPolizas.size();j++) {
			if(idPoliza.equalsIgnoreCase(listaPolizas.get(j).getIdPoliza())){
					listaRecibos.get(i).setTienePoliza(true);
					break;
			}
		}
	}		
	}
}	

public static Date generarMes(String mes) {
	
	
	int dia=30;
	
	if(mes.endsWith("01")||mes.endsWith("03")||mes.endsWith("05")||mes.endsWith("07")||mes.endsWith("08")||mes.endsWith("10")||mes.endsWith("12"))
		dia=31;
	if(mes.endsWith("02"))
		dia=28;
	
	String intMes=mes.substring(4, 6);
	String anio=mes.substring(0, 4);
	
	SimpleDateFormat objSDF = new SimpleDateFormat("dd/MM/yyyy"); 
	
	Date fecha=new Date();
	try {
		
		fecha = objSDF.parse(dia+"/"+intMes+"/"+anio);
	} catch (ParseException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	return fecha;
	
}

public static boolean hayAnuladoPosterior(RecibosMes datosMes,int posicion) {
	
String numRecibo=datosMes.getRecibosMes().get(posicion).getIdRecibo();
	
	for(int i=0;i<datosMes.getRecibosMes().size();i++) {
		
		if(datosMes.getRecibosMes().get(i).getIdRecibo().equalsIgnoreCase(numRecibo) && (datosMes.getRecibosMes().get(i).getSituacionRecibo().equalsIgnoreCase("AN")||datosMes.getRecibosMes().get(i).getSituacionRecibo().equalsIgnoreCase("DE")))
				return true;
	}
	return false;
	
}

public static RecibosMes generarImporteMes(RecibosMes datosMes,Date fechaFinMes,int indiceMes) {
	
	
	Double importeMes= new Double(0.00);
	Double importeTotalMes= new Double(0.00);
	SimpleDateFormat objSDF = new SimpleDateFormat("yyyy-MM-dd"); 
	for(int i=0;i<datosMes.getRecibosMes().size();i++) {
		
		if(datosMes.getRecibosMes().get(i).idRecibo.contains("4615431150"))
			System.out.println("DENTRO");
		
		String estado=datosMes.getRecibosMes().get(i).getSituacionRecibo();
		if(estado!=null && datosMes.getRecibosMes().get(i)!=null) {
			
	if(estado.equalsIgnoreCase("CO") && !datosMes.getRecibosMes().get(i).isAnulado() && datosMes.getRecibosMes().get(i).isActivo(fechaFinMes)) {//datosMes.getRecibosMes().get(i).isValido()!=false) {//!hayAnuladoPosterior(datosMes,i)) {
			
			Date dateInicio;
			try {
				dateInicio = objSDF.parse(datosMes.getRecibosMes().get(i).getFechaSituacion());
				Date dateFin=objSDF.parse(datosMes.getRecibosMes().get(i).getFechaVencimiento());
				
				int dias=0;
				if(fechaFinMes.getTime()>dateFin.getTime())
					dias=daysBetween(dateInicio,dateFin)+2;
				else
					dias=daysBetween(dateInicio,fechaFinMes)+2;	

				importeMes+=datosMes.getRecibosMes().get(i).getImportePorDia()*dias;
				importeTotalMes+=datosMes.getRecibosMes().get(i).getPrimaNeta();
				
				datosMes.getRecibosMes().get(i).setFechaUltimaContablizacion(fechaFinMes);	
				datosMes.getRecibosMes().get(i).anadirContabilizado(datosMes.getRecibosMes().get(i).getImportePorDia()*dias);
				
				DatoMensual datoMes= new DatoMensual();
				datoMes.setFechaMes(fechaFinMes);
				datoMes.setImporteMes(datosMes.getRecibosMes().get(i).getImportePorDia()*dias);
				datosMes.getRecibosMes().get(i).getDatoMeses().add(datoMes);
				
				
				datosMes.getRecibosMes().get(i).setContabilizaEnMes(datosMes.getRecibosMes().get(i).getImportePorDia()*dias);
				datosMes.setImporteMes(importeMes);
				datosMes.setImporteTotal(importeTotalMes);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	
	else if(estado.equalsIgnoreCase("AN")|| estado.equalsIgnoreCase("DE")) {
	String idPoliza=datosMes.getRecibosMes().get(i).getIdPoliza();
	String idRecibo=datosMes.getRecibosMes().get(i).getIdRecibo();
	String numSuplemento=datosMes.getRecibosMes().get(i).getNumeroSuplemento();
		if(i>0) {
		for(int j=i-1;j>=0;j--) {
			
			String idPolizaAnt=datosMes.getRecibosMes().get(j).getIdPoliza();
			String idReciboAnt=datosMes.getRecibosMes().get(j).getIdRecibo();
			String numSuplementoAnt=datosMes.getRecibosMes().get(j).getNumeroSuplemento();

			if(idPoliza!=null && idRecibo!=null && numSuplemento!=null) {
			
				if(idPoliza.equalsIgnoreCase(idPolizaAnt)&& idRecibo.equalsIgnoreCase(idReciboAnt)&& numSuplemento.equalsIgnoreCase(numSuplementoAnt)) {
	
					
					datosMes.getRecibosMes().get(j).setAnulado(true);
					datosMes.getRecibosMes().get(j).setContabilizaEnMes(0.00);
				
					if(datosMes.getRecibosMes().get(j).getSituacionRecibo()!=null && datosMes.getRecibosMes().get(j).getSituacionRecibo().equalsIgnoreCase("CO")) {
						importeTotalMes-=datosMes.getRecibosMes().get(j).getPrimaNeta();
						
						importeMes-=datosMes.getRecibosMes().get(j).getContabilizaEnMes();
						datosMes.setImporteMes(importeMes);
						datosMes.setImporteTotal(importeTotalMes);
						}
					}		
				}
			}		
	}
		
		for(int l=indiceMes-1;l>=0; l--) {
			
			
			
			RecibosMes datosMesAnterior=contabilizacionesMes.get(l);
			
			for(int m=datosMesAnterior.getRecibosMes().size()-1;m>=0;m--) {
				
				String idPolizaAnt=datosMesAnterior.getRecibosMes().get(m).getIdPoliza();
				String idReciboAnt=datosMesAnterior.getRecibosMes().get(m).getIdRecibo();
				
					
				String numSuplementoAnt=datosMesAnterior.getRecibosMes().get(m).getNumeroSuplemento();
				if(idPoliza!=null &&idRecibo!=null && numSuplemento!=null) {
					
				if(idPoliza.equalsIgnoreCase(idPolizaAnt)&& idRecibo.equalsIgnoreCase(idReciboAnt)&& numSuplemento.equalsIgnoreCase(numSuplementoAnt)) {
					datosMesAnterior.getRecibosMes().get(m).setAnulado(true); 
					if(datosMesAnterior.getRecibosMes().get(m).getSituacionRecibo().equalsIgnoreCase("CO") && datosMesAnterior.getRecibosMes().get(m).isActivo(fechaFinMes) && !datosMesAnterior.getRecibosMes().get(m).anulado) {
						Double importeContabilizado=datosMes.getRecibosMes().get(m).getTotalContabilizado();
						datosMes.getRecibosMes().get(i).setImporteReduce(importeContabilizado);
						Double importeMesReduce=datosMes.getImporteRestaMes();
						importeMesReduce+=importeContabilizado;
						datosMes.setImporteRestaMes(importeMesReduce);
						
						
						DatoMensual datoMes= new DatoMensual();
						datoMes.setFechaMes(fechaFinMes);
						datosMes.setImporteMes(-1*datosMesAnterior.getRecibosMes().get(m).getTotalContabilizado());
						datosMes.getRecibosMes().get(i).getDatoMeses().add(datoMes);
						
				 }
			}
			}
				
		}
			
	}
		
	}
	}	
}
	
	return datosMes;
	
}

public static void generarImportesMesesAnteriores(RecibosMes datosMes,Date fechaFinMes,int indiceMes) {
	
	String fechaInicioMes=String.valueOf(fechaFinMes.getYear())+"-"+String.valueOf(fechaFinMes.getMonth()+1)+"-01";
	
	SimpleDateFormat objSDF = new SimpleDateFormat("yyyy-MM-dd"); 
	for(int i=indiceMes-1;i>=0;i--) {
	
		RecibosMes datosMesAnterior= contabilizacionesMes.get(i);
		
		for(int j=datosMesAnterior.getRecibosMes().size()-1;j>=0;j--) {
			
		Recibo recibo=datosMesAnterior.getRecibosMes().get(j);
	//	if(datosMes.getRecibosMes().get(i).idRecibo.contains("4615431150"))
	//	System.out.println("DENTRO");
		if(recibo.getSituacionRecibo()!=null && recibo.getSituacionRecibo().equalsIgnoreCase("CO") && recibo.isActivo(fechaFinMes) && !recibo.anulado) {
			
			Date dateInicio;
			try {
				dateInicio = objSDF.parse(fechaInicioMes);
			
			Date dateFin=objSDF.parse(recibo.getFechaVencimiento());
				
			int dias=0;
			if(fechaFinMes.getTime()>dateFin.getTime())
				dias=daysBetween(dateInicio,dateFin)+2;
				else
					dias=daysBetween(dateInicio,fechaFinMes)+2;	

			try {
			Double	importeMes=contabilizacionesMes.get(i).getRecibosMes().get(j).getImportePorDia()*dias;
				
			DatoMensual datoMes= new DatoMensual();
			datoMes.setFechaMes(fechaFinMes);
			datosMes.setImporteMes(importeMes);

			contabilizacionesMes.get(i).getRecibosMes().get(j).getDatoMeses().add(datoMes);
			} catch (Exception e) {
				System.out.println(e);
			}
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	
}
}

public static void generarImportesMes() {
	
	for (int i=0;i<contabilizacionesMes.size();i++) {
		String  mes = contabilizacionesMes.get(i).getMesContable();
		Date fechaFinMes=generarMes(mes);
		SimpleDateFormat objSDF = new SimpleDateFormat("yyyy-MM-dd"); 
		
		System.out.println(mes+" "+objSDF.format(fechaFinMes));
		RecibosMes nuevoReciboMes=generarImporteMes(contabilizacionesMes.get(i),fechaFinMes,i);
		contabilizacionesMes.set(i, nuevoReciboMes);
		if(i>0)
		generarImportesMesesAnteriores(contabilizacionesMes.get(i),fechaFinMes,i);
		
		
	}
}

public static void escribirResultados() {
	
    FileWriter fichero = null;
    PrintWriter pw = null;
    try
    {
        fichero = new FileWriter("d:\\Resultado2.csv");
        pw = new PrintWriter(fichero);
        
        pw.println("MesContable;IdRecibo;TienePoliza;EsUltimo;EsValido;EstaAnulado;Importe por día;Importe total;EstadoRecibo;TotalContabilizado;TotalRechazado;FechaSituacion;2021-04;2021-05;2021-06;2021-07;2021-08;2021-09;2021-10;2021-11;2021-12;2022-01;2022-02;2022-03;2022-04;2022-05;2022-06;2022-07;2022-08;2022-09;2022-10;2022-11;2022-12;2023-01;2023-02;2023-03;2023-04;2023-05;2023-06;2023-07;2023-08;2023-09;2023-10;2023-11;;2023-12");
    	for(int i=0;i<contabilizacionesMes.size();i++) {
    	//	System.out.println("-------------------------------------------------------------------------");
    	//	System.out.println(contabilizacionesMes.get(i).getMesContable()+" :"+contabilizacionesMes.get(i).getImporteMes()+ " "+contabilizacionesMes.get(i).getImporteTotal());
    	for(int j=0;j<contabilizacionesMes.get(i).getRecibosMes().size();j++) {
    		
    		
    		pw.println(contabilizacionesMes.get(i).getMesContable()+";"+contabilizacionesMes.get(i).getRecibosMes().get(j).getIdRecibo()+";"+contabilizacionesMes.get(i).getRecibosMes().get(j).isTienePoliza()+";"+contabilizacionesMes.get(i).getRecibosMes().get(j).isUltimo()+";"+contabilizacionesMes.get(i).getRecibosMes().get(j).isValido()+";"+contabilizacionesMes.get(i).getRecibosMes().get(j).isAnulado()+";"+String.valueOf(contabilizacionesMes.get(i).getRecibosMes().get(j).getImportePorDia()).replace(".", ",")+";"+String.valueOf(contabilizacionesMes.get(i).getRecibosMes().get(j).getPrimaNeta()).replace(".", ",")+";"
    	+contabilizacionesMes.get(i).getRecibosMes().get(j).getSituacionRecibo()+";"+String.valueOf(contabilizacionesMes.get(i).getRecibosMes().get(j).getTotalContabilizado()).replace(".",",")+";"+contabilizacionesMes.get(i).getRecibosMes().get(j).getImporteReduce()+";"+contabilizacionesMes.get(i).getRecibosMes().get(j).getFechaSituacion()
    	+";"+generaLineaMeses(contabilizacionesMes.get(i).getRecibosMes().get(j)));
    	}
    	}
      
    } catch (Exception e) {
        e.printStackTrace();
    } finally {
       try {
       // Nuevamente aprovechamos el finally para 
       // asegurarnos que se cierra el fichero.
       if (null != fichero)
          fichero.close();
       } catch (Exception e2) {
          e2.printStackTrace();
       }
    }
	
}

public static String generaLineaMeses(Recibo recibo) {
	SimpleDateFormat objSDF = new SimpleDateFormat("yyyy-MM-dd"); 
	String cadena="";
boolean puesto=false;
	
	for(int i=22;i<recibo.getDatoMeses().size();i++) {
		String fecha= objSDF.format(recibo.getDatoMeses().get(i).getFechaMes());
		int anioFechaMes=Integer.valueOf(fecha.substring(0,4));
		int mesFechaMes=Integer.valueOf(fecha.substring(5,7));
		int anioVencimiento=Integer.valueOf(recibo.getFechaVencimiento().substring(0,4));
		int mesVencimiento=Integer.valueOf(recibo.getFechaVencimiento().substring(5,7));
		if(anioVencimiento>anioFechaMes &&!puesto) {
			System.out.println(recibo.getDatoMeses().get(i).getImporteMes());
			cadena+=String.valueOf(recibo.getDatoMeses().get(i).getImporteMes())+";";
		
		}
		else
			if(anioVencimiento==anioFechaMes && mesVencimiento>=mesFechaMes) {
				System.out.println(recibo.getDatoMeses().get(i).getImporteMes());
				puesto=true;
				cadena+=String.valueOf(recibo.getDatoMeses().get(i).getImporteMes())+";";
			}
	}
	return cadena;
}
public int obtenerUltimoDiaMes (int anio, int mes) {
	 
	Calendar calendario=Calendar.getInstance();
	calendario.set(anio, mes-1, 1);
	return calendario.getActualMaximum(Calendar.DAY_OF_MONTH);
	 
	}

public String obtenerMayorFechaVencimiento() {
	
	String fechaInicial="0001-01-01";
	SimpleDateFormat objSDF = new SimpleDateFormat("yyyy-MM-dd"); 
	for(int i=0;i<listaRecibos.size();i++) {
	
		int anioFechaInicial=Integer.valueOf(fechaInicial.substring(0,4));
		int mesFechaInicial=Integer.valueOf(fechaInicial.substring(5,7));
		int anioVencimiento=Integer.valueOf(listaRecibos.get(i).getFechaVencimiento().substring(0,4));
		int mesVencimiento=Integer.valueOf(listaRecibos.get(i).getFechaVencimiento().substring(5,7));
	if(anioVencimiento>anioFechaInicial || (anioVencimiento==anioFechaInicial && mesVencimiento>=mesFechaInicial)) {
		int dia=obtenerUltimoDiaMes (anioVencimiento,mesVencimiento);
		fechaInicial= listaRecibos.get(i).getFechaVencimiento().substring(0,4)+"-"+listaRecibos.get(i).getFechaVencimiento().substring(5,7)+"-"+String.valueOf(dia);
	}
		
	}
	return fechaInicial;
	}


public static ArrayList<String> obtenerListaInserts(String rutaInserts) {
	
	// Fichero del que queremos leer
			File fichero = new File(rutaInserts);
			
			ArrayList<String> listaRetorno= new ArrayList();
			Scanner s = null;

			try {
				// Leemos el contenido del fichero
				System.out.println("... Leemos el contenido del fichero ...");
				s = new Scanner(fichero);

				// Leemos linea a linea el fichero
				while (s.hasNextLine()) {
					String linea = s.nextLine().trim(); 	// Guardamos la linea en un String
					listaRetorno.add(linea);     // Imprimimos la linea

				}

			} catch (Exception ex) {
				System.out.println("Mensaje: " + ex.getMessage());
			} finally {
				// Cerramos el fichero tanto si la lectura ha sido correcta o no
				try {
					if (s != null)
						s.close();
					return listaRetorno;
				} catch (Exception ex2) {
					
					System.out.println("Mensaje 2: " + ex2.getMessage());
					return null;
				}
			}
	
}


public static ArrayList<String> subirInserts(String rutaInserts){
	 URL urlForGetRequest;
	 ArrayList listaInserts=obtenerListaInserts(rutaInserts);
	 ArrayList<String> listaRetornos=new ArrayList();
		try {
			
			  TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
		           
					@Override
					public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
						// TODO Auto-generated method stub
						
					}

					@Override
					public void checkClientTrusted(X509Certificate[] chain, String authType)
							throws CertificateException {
						// TODO Auto-generated method stub
						
					}

					@Override
					public X509Certificate[] getAcceptedIssuers() {
						// TODO Auto-generated method stub
						return null;
					}
		        }
		        };

		        // Install the all-trusting trust manager
		        SSLContext sc = SSLContext.getInstance("SSL");
		        sc.init(null, trustAllCerts, new java.security.SecureRandom());
		        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

		        // Create all-trusting host name verifier
		        HostnameVerifier allHostsValid = new HostnameVerifier() {
		            
					@Override
					public boolean verify(String arg0, SSLSession arg1) {
						// TODO Auto-generated method stub
						return true;
					}
		        };

		        // Install the all-trusting host verifier
		      HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
	for(int i=0;i<listaInserts.size();i++) {		
		//	urlForGetRequest = new URL("https://concomseg-sftp.openbank.gs.corp/sftp/lista/in");
		System.out.println(i+" / "+listaInserts.size());
		String url="https://agps-comcomseg-sftp-es.dev.ok-cloud.net/db/collections?query="+URLEncoder.encode((String) listaInserts.get(i));
			urlForGetRequest = new URL(url);
		//	urlForGetRequest = new URL("https://agps-comcomseg-sftp-es.dev.ok-cloud.net/db/collections?query=select%20*%20from%20seguros_comis_contab");
		     String readLine = null;

		     HttpsURLConnection conection = (HttpsURLConnection) urlForGetRequest.openConnection();
		   
		     conection.setRequestMethod("GET");

		 //    conection.setRequestProperty("userId", "a1bcdef"); // set userId its a sample here

		     int responseCode = conection.getResponseCode();

		     if (responseCode == HttpURLConnection.HTTP_OK) {
               } else {
		    	 System.out.println("GET NOT WORKED-->"+url);
		    	 listaRetornos.add(url);
		    	 
               }	
     }
		     
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return null;
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			return null;
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			return null;
		}
		return listaRetornos;
		 
}
	
	public static void main(String[] args) {
		
//	subirInserts("d:\\listaInserts2.txt");
		// TODO Auto-generated method stub
	    datosMeses= new DatosMeses();
	    listaPolizas=new ArrayList<Poliza>();
		listaRecibos=new ArrayList<Recibo>();
		
		ArrayList<String> listadoFicheros= null;
		
		if(remoto) {
			listadoFicheros=obtenerListadoFicheros();
			recuperarFicherosDaily("DatosDaily.txt");
			recuperarFicherosDaily("FicherosDaily.txt");
		}
		else {
			listadoFicheros=obtenerListadoFicheros(rutaFicheros);
			recuperarFicherosDailyLocal("DatosDaily.txt");
			recuperarFicherosDailyLocal("FicherosDaily.txt");
		}
		
	
		 
		if(listadoFicheros!=null) {
		
			recuperarFicheros(listadoFicheros);
			
		}
System.out.println();

rellenarSiPoliza();

generarEstadisticaMes();
generarImportesMes();
escribirResultados();
escribirPolizas("C:/TEMP/polizas.csv");	
escribirRecibos("C:/TEMP/recibos.csv");
	}
	}


