import React from "react";
import './css/footer.css'

const PageFooter = () => (
    <footer id="footer" role="contentinfo">
        <div className="powered-by">
            powered-by			
            <a href="http://www.github.com/eclipse/sw360" rel="external" target="_blank">SW360</a> |
            <a href="/resource/mkdocs/index.html" rel="external" target="_blank">SW360 Docs</a> |
            <a href="/resource/docs/api-guide.html" rel="external" target="_blank">REST API Docs</a> |
            <a href="https://github.com/eclipse/sw360/issues" rel="external" target="_blank"> Report an issue.</a>
        </div>
        <div className="build-info text-muted">
            Version: 16.0.0 | Branch: UNKNOWN (d15db4a) | Build time: 2023-02-13T02:10:24Z
        </div>
    </footer>
)

export default PageFooter