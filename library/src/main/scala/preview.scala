package pamflet

import unfiltered.request._
import unfiltered.response._
import java.io.OutputStream
import java.net.URI

object Preview {
  def apply(contents: => Contents) = {
    def css = Map.empty ++ contents.css
    def files = Map.empty ++ contents.files 
    unfiltered.jetty.Http.anylocal.filter(unfiltered.filter.Planify {
      case GET(Path(Seg(Nil))) =>
        contents.pages.headOption.map { page =>
          Redirect("/" + Printer.webify(page))
        }.getOrElse { NotFound }
      case GET(Path(Seg("favicon.ico" :: Nil))) if contents.favicon.isDefined =>
        contents.favicon map { responseStreamer } getOrElse { NotFound }
      case GET(Path(Seg(name :: Nil))) =>
        Printer(contents, None).printNamed(name).map { html =>
          Html5(html)
        }.getOrElse { NotFound }
      case GET(Path(Seg("css" :: name :: Nil))) if css.contains(name) =>
        CssContent ~> ResponseString(css(name))
      case GET(Path(Seg("files" :: name :: Nil))) if files.contains(name) =>
        responseStreamer(files(name))
    }).resources(Shared.resources)
  }
  def responseStreamer(uri: URI) =
    new ResponseStreamer { def stream(os:OutputStream) { 
      val is = uri.toURL.openStream
      try {
        val buf = new Array[Byte](1024)
        Iterator.continually(is.read(buf)).takeWhile(_ != -1)
          .foreach(os.write(buf, 0, _))
      } finally {
        is.close
      }
    } }
}
