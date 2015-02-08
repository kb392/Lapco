package ru.swimming.forum.lapco;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import android.widget.Toast;

public class OpenWriter {
	MainActivity context;
    Document domDoc;
    private ZipOutputStream zipOutputStream;
	
	public OpenWriter(Context c){
		context= (MainActivity) c;
		Log.d(null,"OpenWriter created");
	}

	public boolean WriteResult() {
        AssetManager assetManager = context.getAssets();
        InputStream inputStream = null;
        
        try {
            inputStream = assetManager.open("content.xml"); //

        } catch (IOException e) {
            Log.e(null, e.getMessage());
            return false;
        }
        try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
    		Log.d(null,"Parsing xml");
    		domDoc = builder.parse(inputStream);
			Log.d(null,"Read xml " + domDoc.getXmlVersion());
			XPath xpath = XPathFactory.newInstance().newXPath();
			
			xpath.setNamespaceContext(new MyNamespaceContext());
		    String strXPath = "//table:table-row[@lapco:id = 'headrow']/table:table-cell/text:p";
		    NodeList nodes = (NodeList) xpath.evaluate(strXPath , domDoc, XPathConstants.NODESET);
		    Log.d(null,strXPath + " - found nodes - "+Integer.toString(nodes.getLength()) );
		    if (nodes.getLength()<4){
		    	Log.e(null,"content template error "+strXPath + " - found nodes - "+Integer.toString(nodes.getLength()));
		    	return false;
		    }
		    
		    nodes.item(0).setTextContent(context.getString(R.string.lap));
		    nodes.item(1).setTextContent(context.getString(R.string.distance));
		    nodes.item(2).setTextContent(context.getString(R.string.distance_time));
		    nodes.item(3).setTextContent(context.getString(R.string.lap_time));
		    
		    strXPath = "//table:table//table:table-row[2]";
		    nodes = (NodeList) xpath.evaluate(strXPath , domDoc, XPathConstants.NODESET);
		    Node exampleRow = nodes.item(0);
		    Node newRow;
		    for (int i=0;i<context.aLoopStop.length-1;i++) {
			    newRow=exampleRow.cloneNode(true);
			    exampleRow.getParentNode().appendChild(newRow);
		    }
		    
		    WriteLapTable(xpath);
		    
		    writeXmlFile("content.xml");

		} catch (ParserConfigurationException e) {
            Log.e(null, "Parser configuration error:" + e.getMessage());

			e.printStackTrace();
            return false;
		} catch (SAXException e) {
            Log.e(null, "SAX error: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
            Log.e(null, "IO error: " + e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
            Log.e(null, "Some error: " + e.getMessage());
			e.printStackTrace();
		}
		return true;
	}
	
	
	private Node getXmlTableCell(XPath xpath, int iRow, int iCol) {
		String strXPath=String.format("//table:table/table:table-row[%d]/table:table-cell[%d]",iRow,iCol); ///text:p
		NodeList nodes;
		try {
			nodes = (NodeList) xpath.evaluate(strXPath , domDoc, XPathConstants.NODESET);
			return nodes.item(0); 		   
		} catch (XPathExpressionException e) {
            Log.e(null, "XPath ["+strXPath+"] error: " + e.getMessage());
			e.printStackTrace();
		}
		return null; 		   
	}
	
	private void CellWriteInt(Node node, int iValue){
		Element elem=(Element)node;
		
		Log.d("xml","office:value"+"="+ Integer.toString(iValue));
		elem.setAttribute("office:value", Integer.toString(iValue));
		Log.d("xml","office:value-type"+"="+ "float");
		elem.setAttribute("office:value-type", "float");
		//elem.setAttribute("lapco:writed", "true");
		
		Node nodeTxtValue=elem.getOwnerDocument().createElement("text:p");
		nodeTxtValue.appendChild(elem.getOwnerDocument().createTextNode(Integer.toString(iValue)));
		elem.appendChild(nodeTxtValue);
	}
	
	private void CellWriteTime(Node node, long iValue){
		Element elem=(Element)node;
		
		elem.setAttribute("office:time-value", context.GetFormatedTime(iValue,2));
		elem.setAttribute("office:value-type", "time");
		//elem.setAttribute("lapco:writed", "true");

		Node nodeTxtValue=elem.getOwnerDocument().createElement("text:p");
		nodeTxtValue.appendChild(elem.getOwnerDocument().createTextNode(context.GetFormatedTime(iValue,0)));
		elem.appendChild(nodeTxtValue);
	
	}
	
	
	private void WriteLapTable(XPath xpath) {
		Node nodeCell;
		for (int i=0;i<context.aLoopStop.length;i++ ) {
			nodeCell=getXmlTableCell(xpath, i+2,1);
			CellWriteInt(nodeCell,i+1);

			nodeCell=getXmlTableCell(xpath, i+2,2);
			CellWriteInt(nodeCell,context.iLapLen*(i+1));
			
			nodeCell=getXmlTableCell(xpath, i+2,3);
			CellWriteTime(nodeCell,context.aLoopStop[i]);
			  
			nodeCell=getXmlTableCell(xpath, i+2,4);
			CellWriteTime(nodeCell,((i == 0) ? context.aLoopStop[i]
					: (context.aLoopStop[i] - context.aLoopStop[i - 1])));
		}
	}

	
    private static class MyNamespaceContext implements NamespaceContext {

        public String getNamespaceURI(String prefix) {
        	//быдлокод :)
            if("lapco".equals(prefix)) {
                return "http://forum.swimming.ru/Lapco";
            }
            if("office".equals(prefix)) {
                return "urn:oasis:names:tc:opendocument:xmlns:office:1.0";
            }
            if("table".equals(prefix)) {
                return "urn:oasis:names:tc:opendocument:xmlns:table:1.0";
            }
            if("text".equals(prefix)) {
                return "urn:oasis:names:tc:opendocument:xmlns:text:1.0";
            }

            return null;
        }

        public String getPrefix(String namespaceURI) {
        	//быдлокод :)
            if("http://forum.swimming.ru/Lapco".equals(namespaceURI)) {
                return "lapco";
            }
            if("urn:oasis:names:tc:opendocument:xmlns:office:1.0".equals(namespaceURI)) {
                return "office";
            }
            if("urn:oasis:names:tc:opendocument:xmlns:table:1.0".equals(namespaceURI)) {
                return "table";
            }
            if("urn:oasis:names:tc:opendocument:xmlns:text:1.0".equals(namespaceURI)) {
                return "text";
            }
            return null;
        }

        public Iterator getPrefixes(String namespaceURI) {
            return null;
        }

    }
	
	private String readTextFile(InputStream inputStream) {
	    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

	    byte buf[] = new byte[1024];
	    int len;
	    try {
	        while ((len = inputStream.read(buf)) != -1) {
	            outputStream.write(buf, 0, len);
	        }
	        outputStream.close();
	        inputStream.close();
	    } catch (IOException e) {

	    }
	    return outputStream.toString();
	}
	
	private void AssetToZip(String strAssetName) {
        AssetManager assetManager = context.getAssets();
        InputStream inputStream = null;
        
            try {
				inputStream = assetManager.open(strAssetName);
				InputStreamToZip(inputStream, strAssetName);
			} catch (IOException e) {
				Log.e("ods","asset to zip read error: " + e.getMessage());
				e.printStackTrace();
			} catch (Exception e) {
				Log.e("ods","asset to zip error: " + e.getMessage());
				e.printStackTrace();
			} //
	}

	
	private void InputStreamToZip(InputStream is, String strZipName) {
		
        int iLength;
        byte[] buff = new byte[1024];
        
        Log.d("ods","Zipping "+strZipName);
        try {
			zipOutputStream.putNextEntry(new ZipEntry(strZipName));
        while ((iLength = is.read(buff)) > 0) {
        	zipOutputStream.write(buff, 0, iLength );
        	}
        	 
        zipOutputStream.closeEntry();
        is.close();
		} catch (IOException e) {
			Log.e("zip","zip writing error: " + e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			Log.e("zip","zip error: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	public void MakeOds(File fContent) {
		try {
			String strZipFileName=context.getAppFolder(context.getExternalFilesDir(null).getAbsolutePath())+"/"+context.getFileName()+".ods";
	        FileInputStream fis = new FileInputStream(fContent);
            //FileOutputStream fos = new FileOutputStream(strZipFileName);
	        //zipOutputStream = new ZipOutputStream(fos);
	        zipOutputStream = new ZipOutputStream(new FileOutputStream(strZipFileName));
	        InputStreamToZip(fis, fContent.getName()); //content.xml
	        AssetToZip("mimetype");
	        AssetToZip("META-INF/manifest.xml");
	        AssetToZip("settings.xml");
	        AssetToZip("styles.xml");
            zipOutputStream.close();
            Toast.makeText(context, context.getString(R.string.saved) +" " + strZipFileName, Toast.LENGTH_LONG).show();
		} catch (IOException e) {
			Log.e("ods",".ods writing error: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public void writeXmlFile(String strFileName) {
	    try {
	        // Prepare the DOM document for writing
	        Source source = new DOMSource(domDoc);

	        //File file = new File(Environment.getExternalStorageDirectory(), strFileName);
	        Log.d(null, "Writing "+strFileName+" to "+context.getExternalFilesDir(null));
			File f = new File(context.getExternalFilesDir(null), strFileName);

	        Result result = new StreamResult(f);

	        // Write the DOM document to the file
	        Transformer xformer = TransformerFactory.newInstance().newTransformer();
	        xformer.transform(source, result);
	        MakeOds(f);
	    } catch (TransformerConfigurationException e) {
	    	Log.e(null,"XML Transformer Configuration error: "+ e.getMessage());
	    } catch (TransformerException e) {
	    	Log.e(null,"XML Transformer error: "+ e.getMessage());
		} catch (Exception e) {
			Log.e(null,"XML Write error: "+ e.getMessage());
		}
	}
}
