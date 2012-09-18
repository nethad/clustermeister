%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% uzhpub.cls
%% Copyright 2010 Jürgen Spitzmüller <spitzmueller@ds.uzh.ch>
%
% This work may be distributed and/or modified under the
% conditions of the LaTeX Project Public License, either version 1.3
% of this license or (at your option) any later version.
% The latest version of this license is in
%   http://www.latex-project.org/lppl.txt
% and version 1.3 or later is part of all distributions of LaTeX
% version 2003/12/01 or later.
%
% This work has the LPPL maintenance status "maintained".
% 
% The Current Maintainer of this work is Jürgen Spitzmüller.
%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% This class file builds on the scrartcl class
%% the KOMA Script bundle
%% 
%% This class implements the layout as defined by
%% the new (2010) corporate identity of the
%% University of Zurich (UZH), English version.
%%
%% USAGE: \documentclass[uzh-en]{uzhpub}
%%
%% Author data (Institute) can be adjusted at the bottom
%% of this file.
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

\def\filedate{2011/09/08}
\def\fileversion{1.2}
\def\filename{uzhpub.cls}
\NeedsTeXFormat{LaTeX2e}[1997/06/01]

\ProvidesClass{uzhpub}[\filedate\space v\fileversion\space UZH publication template (JSp)]


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% GENERAL SETTINGS
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

%% FONTS (URW Arial and Mathpazo; available at CTAN)
\RequirePackage{uarial}
\RequirePackage{mathpazo}

%% BASE CLASS
\LoadClass{scrartcl}[1997/04/16]

%% BASE FONT SIZE (10pt/14pt)
\KOMAoptions{fontsize=10pt}
\renewcommand\normalsize{%
   \@setfontsize\normalsize\@xpt\@xivpt
   \abovedisplayskip 10\p@ \@plus2\p@ \@minus5\p@
   \abovedisplayshortskip \z@ \@plus3\p@
   \belowdisplayshortskip 6\p@ \@plus3\p@ \@minus3\p@
   \belowdisplayskip \abovedisplayskip
   \let\@listi\@listI}


%% OPTIONS
\DeclareOption{10pt}{\KOMAoptions{fontsize=10pt}}
\DeclareOption{11pt}{\KOMAoptions{fontsize=11pt}}
\DeclareOption{12pt}{\KOMAoptions{fontsize=12pt}}
\def\sprache{}
\newif\ifuzh@english
\uzh@englishfalse

\DeclareOption{english}{
  \def\sprache{english}
  \uzh@englishtrue
}
\DeclareOption{american}{
  \def\sprache{english}
  \uzh@englishtrue
}
\DeclareOption{german}{
  \def\sprache{german}
}
\DeclareOption{ngerman}{
  \def\sprache{ngerman}
}

\newif\ifbeamer\beamerfalse
\def\beamer{\beamertrue}

\DeclareOption{beamer}{\beamertrue}

\DeclareOption{titlepage}{\KOMAoptions{titlepage=true}}

\ProcessOptions


%% UZH LOGO
\RequirePackage{graphicx}
\newcommand*\uzhlogo{\includegraphics{uzh_logo_d_pos}}
\ifuzh@english
      \renewcommand*\uzhlogo{\includegraphics{uzh_logo_e_pos}}
\fi

%% PAGE LAYOUT AND DIMENSIONS
\RequirePackage{geometry}
\geometry{%
  a4paper,
  top=38mm,
  left=35mm,
  right=16mm,
  bottom=24mm,
  headsep=26mm,
  footskip=15mm
}

\setlength{\columnsep}{5mm}


%% FONT SIZES
\setkomafont{pagefoot}{\fontsize{7.5}{9pt}\selectfont\upshape}
\setkomafont{pagehead}{\fontsize{7.5}{9pt}\selectfont\upshape}
\setkomafont{title}{\fontsize{20}{24pt}\selectfont\upshape\bfseries}
\setkomafont{title}{\fontsize{12}{15pt}\selectfont\upshape\bfseries}
\setkomafont{section}{\fontsize{15}{17pt}\selectfont\rmfamily\upshape\bfseries}
\setkomafont{subsection}{\fontsize{10}{14pt}\selectfont\rmfamily\upshape\bfseries}
\setkomafont{subsubsection}{\fontsize{10}{14pt}\selectfont\rmfamily\upshape\bfseries}
\setkomafont{part}{\fontsize{15}{17pt}\selectfont\rmfamily\upshape\bfseries\underline}

%% FOOTNOTES
\RequirePackage[ragged,bottom,hang,norule]{footmisc}
\renewcommand\footnotelayout{\fontsize{7.5}{9pt}\selectfont}
\renewcommand\footnotemargin{.8em}
\setlength{\footnotesep}{12pt}

%% LISTS
\def\labelitemi{\normalfont{--}}
\def\labelitemii{\normalfont{--}}
\def\labelitemiii{\normalfont{--}}
\def\labelitemiv{\normalfont{--}}
\RequirePackage{enumitem}
\setlist{leftmargin=*,nolistsep}
\setitemize{labelwidth=2.7mm}


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% PAGE STYLES
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

%% MACROS
\newcommand*\uzh{}
\newcommand*\uzhunit{}
\def\partsave{}

%% REDEFINE \addpart
\renewcommand*{\@addpart}{}
\def\@addpart[#1]#2{%
  \csname phantomsection\endcsname
  \addcontentsline{toc}{part}{#1}\@spart{#2}%
  \ifx\partmark\@gobble\else\@mkboth{#1}{}\fi
  \def\partsave{#1}
}

%% HEADER (FIRST PAGE)
\def\uzhhead{%
    \hspace*{-1.95cm}\uzhlogo
    \ifuzh@english
    \put(-100,-52.5){\parbox[t][40mm][t]{\textwidth}{\raggedright
      {\fontsize{12}{14pt}\selectfont\textup{\textbf{\uzhunit}}\\\vspace*{-1pt}\hrulefill}%
      }}%
    \else
    \put(-86.5,-52.5){\parbox[t][40mm][t]{\textwidth}{\raggedright
      {\fontsize{12}{14pt}\selectfont\textup{\textbf{\uzhunit}}\\\vspace*{-1pt}\hrulefill}%
      }}%
    \fi
}

%% HEADER (SUBSEQUENT PAGES)
\def\uzhffhead{%
    \ifx\partsave\empty
       \parbox{\textwidth}{\vspace*{12pt}\headmark\hfill\@title}%
    \else
        \parbox{\textwidth}{\vspace*{12pt}\partsave\hfill\@title\newline\headmark}%
    \fi
}

%% SET UP HEADERS WITH SCRPAGE2
\RequirePackage{scrpage2}
\automark[section]{section}
\defpagestyle{uzhheadings}{%
  {\uzhffhead}%
  {\uzhffhead}%
  {\uzhffhead}%
}%
{{\textup\pagename~\thepage\hfill \uzh{}, \uzhunit{}\ifx\@date\@empty\else, \@date\fi}%
 {\textup\pagename~\thepage\hfill \uzh{}, \uzhunit{}\ifx\@date\@empty\else, \@date\fi}%
 {\textup\pagename~\thepage\hfill \uzh{}, \uzhunit{}\ifx\@date\@empty\else, \@date\fi}}

\def\uzhtitlefoot{%
  \if@titlepage
  \else
    \textup\pagename~\thepage\hfill
  \fi
}

\defpagestyle{uzhtitlepage}{%
  {\uzhhead}{\uzhhead}{\uzhhead}%
}%
{{\uzhtitlefoot}{\uzhtitlefoot}{\uzhtitlefoot}}

\pagestyle{uzhheadings}



%% TITLING

\renewcommand*\maketitle[1][1]{%
  \thispagestyle{uzhtitlepage}
  \if@titlepage
    \begin{titlepage}
      \setcounter{page}{%
        #1%
      }%
      \let\footnotesize\small
      \let\footnoterule\relax
      \let\footnote\thanks
      \renewcommand*\thefootnote{\@fnsymbol\c@footnote}%
      \let\@oldmakefnmark\@makefnmark
      \renewcommand*{\@makefnmark}{\rlap\@oldmakefnmark}%
      \ifx\@extratitle\@empty \else
        \noindent\@extratitle\next@tpage\cleardoubleemptypage
%        \thispagestyle{empty}%
      \fi
      \setparsizes{\z@}{\z@}{\z@\@plus 1fil}\par@updaterelative
      \ifx\@titlehead\@empty \else
        \begin{minipage}[t]{\textwidth}%
        \@titlehead
        \end{minipage}\par
      \fi
      \null\vskip 15.5em
        \ifx\@subject\@empty \else
          {\subject@font \@subject \par}%
          \vskip 3em
        \fi
        {\titlefont\huge \@title\par}%
        \vskip .8em
        {\ifx\@subtitle\@empty\else\usekomafont{subtitle}\@subtitle\par\fi}%
        \vskip 1.45em
        {\fontsize{12}{15pt}\selectfont\@author\par}%
        \vskip 3em
      \par
      \@thanks
      \vfill\null
      \if@twoside\next@tpage
        \begin{minipage}[t]{\textwidth}
          \@uppertitleback
        \end{minipage}\par
        \vfill
        \begin{minipage}[b]{\textwidth}
          \@lowertitleback
        \end{minipage}
      \fi
      \ifx\@dedication\@empty \else
        \next@tpage\null\vfill
        {\centering \Large \@dedication \par}%
        \vskip \z@ \@plus3fill
        \if@twoside \next@tpage\cleardoubleemptypage \fi
      \fi
    \end{titlepage}
  \else
    \par
    \@tempcnta=%
    #1%
    \relax\ifnum\@tempcnta=1\else
      \ClassWarning{\KOMAClassName}{%
        Optional argument of \string\maketitle\space ignored
        at\MessageBreak
        notitlepage-mode%
      }%
    \fi
    \begingroup
      \renewcommand*\thefootnote{\@fnsymbol\c@footnote}%
      \let\@oldmakefnmark\@makefnmark
      \renewcommand*{\@makefnmark}{\rlap\@oldmakefnmark}
      \if@twocolumn
        \ifnum \col@number=\@ne
          \@maketitle
        \else
          \twocolumn[\@maketitle]%
        \fi
      \else
        \newpage
        \global\@topnum\z@
        \@maketitle
      \fi
      \@thanks
    \endgroup
  \fi
  \setcounter{footnote}{0}%
  \let\thanks\relax
  \let\maketitle\relax
  \let\@maketitle\relax
  \global\let\@thanks\@empty
  \global\let\@author\@empty
%  \global\let\@date\@empty
%  \global\let\@title\@empty
  \global\let\@subtitle\@empty
  \global\let\@extratitle\@empty
  \global\let\@titlehead\@empty
  \global\let\@subject\@empty
  \global\let\@publishers\@empty
  \global\let\@uppertitleback\@empty
  \global\let\@lowertitleback\@empty
  \global\let\@dedication\@empty
  \global\let\author\relax
  \global\let\title\relax
  \global\let\extratitle\relax
  \global\let\titlehead\relax
  \global\let\subject\relax
  \global\let\publishers\relax
  \global\let\uppertitleback\relax
  \global\let\lowertitleback\relax
  \global\let\dedication\relax
  \global\let\date\relax
  \global\let\and\relax
  \if@twocolumn
     \newgeometry{%
	top=38mm,
	left=16mm,
	right=16mm,
	bottom=24mm,
	headsep=26mm,
	footskip=15mm
      }
  \fi
}%
\renewcommand*{\@maketitle}{%
  \clearpage
  \let\footnote\thanks
  \ifx\@extratitle\@empty \else
    \noindent\@extratitle \next@tpage \if@twoside \null\next@tpage \fi
  \fi
  \setparsizes{\z@}{\z@}{\z@\@plus 1fil}\par@updaterelative
  \ifx\@titlehead\@empty \else
    \begin{minipage}[t]{\textwidth}
      \@titlehead
    \end{minipage}\par
  \fi
  \null
  \vskip 1.25em%
    \ifx\@subject\@empty \else
      {\subject@font \@subject \par}
      \vskip 1.5em%
    \fi
    {\titlefont\huge \@title \par}%
    \vskip 2.5mm%
    {\ifx\@subtitle\@empty\else\usekomafont{subtitle}\@subtitle\par\fi}%
    {\fontsize{12}{15pt}\selectfont~\\\@author\par}%
    \vskip \z@ \@plus 1em
    {\Large \@publishers \par}
    \ifx\@dedication\@empty \else
      \vskip 2em
      {\Large \@dedication \par}
    \fi
  \par
  \vskip 30pt
}%
\renewenvironment{titlepage}{%
  \if@twocolumn
    \@restonecoltrue\onecolumn
  \else
    \@restonecolfalse\newpage
  \fi
  \if@compatibility
    \setcounter{page}{0}%
  \fi
}{%
  \if@restonecol\twocolumn \else \newpage \fi
}%


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% TOC
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

\RequirePackage{tocloft}
\if@titlepage
   \tocloftpagestyle{uzhheadings}
\fi
\renewcommand\cfttoctitlefont{\usekomafont{section}}
\setlength\cftaftertoctitleskip{17pt}
\renewcommand\cftpartfont{\fontsize{10}{14pt}\selectfont\rmfamily\upshape\bfseries}
\renewcommand\cftpartpagefont{\fontsize{10}{14pt}\selectfont\rmfamily\upshape\bfseries}
\setlength\cftsubsecindent{0pt}
\setlength\cftsubsubsecindent{6mm}
\setlength\cftparaindent{14mm}
\newlength\subsubseclinewidth
\setlength\subsubseclinewidth{\textwidth}
\if@twocolumn
   \addtolength\subsubseclinewidth{2.2\cftsubsubsecindent}
\else
   \addtolength\subsubseclinewidth{-\cftsubsubsecindent}
\fi

\renewcommand{\cftpartafterpnum}{\par\hspace*{-\cftpartnumwidth}\rule[2.2ex]{\linewidth}{1pt}}
\renewcommand{\cftsecafterpnum}{\par\hspace*{-\cftsecnumwidth}\rule[2.2ex]{\linewidth}{1pt}}
\renewcommand{\cftsubsecleader}{\hfill}
\renewcommand{\cftsubsecafterpnum}{\par\hspace*{-\cftsubsecnumwidth}\rule[2.2ex]{\linewidth}{.3pt}}
\renewcommand{\cftsubsubsecleader}{\hfill}
\renewcommand{\cftsubsubsecafterpnum}{\par\hspace*{-1.52\cftsubsubsecnumwidth}\rule[2.2ex]{\subsubseclinewidth}{.3pt}}
\renewcommand{\cftparaleader}{\hfill}

\setlength\cftbeforepartskip{.5ex}
\setlength\cftbeforesecskip{.5ex}
\setlength\cftbeforesubsecskip{-2.7ex}
\setlength\cftbeforesubsubsecskip{-2.7ex}

\renewcommand{\@cfttocstart}{%
   \if@twocolumn
      \@restonecoltrue\onecolumn
    \else
      \@restonecolfalse
    \fi
}

\renewcommand{\@cfttocfinish}{%
    \if@restonecol\twocolumn\fi
}

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% DIMENSIONS
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

% ADJUST SPACING FOR \SECTION
\renewcommand\section{\@startsection{section}{1}{\z@}%
  {-2\baselineskip \@plus -1ex \@minus -.2ex}%
  {\baselineskip}%
  {\ifnum \scr@compatibility>\@nameuse{scr@v@2.96}\relax
    \setlength{\parfillskip}{\z@ plus 1fil}\fi
    \raggedsection\normalfont\sectfont\nobreak\size@section}%
}
% ADJUST SPACING FOR \SUBSECTION
\renewcommand\subsection{\@startsection{subsection}{2}{\z@}%
  {-2\baselineskip\@plus -1ex \@minus -.2ex}%
  {\baselineskip}%
  {\ifnum \scr@compatibility>\@nameuse{scr@v@2.96}\relax
    \setlength{\parfillskip}{\z@ plus 1fil}\fi
    \raggedsection\normalfont\sectfont\nobreak\size@subsection
  }%
}


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% MISC
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%


%% BIBLIOGRAPHY REDEFINITIONS

% Allow to set up bibliography title easily
\def\Bibheading#1{\def\bibheading{#1}} \def\bibheading{\refname}

\renewenvironment{thebibliography}[1]{%
  \bib@heading%
  \ifx\bibpreamble\relax\else\ifx\bibpreamble\@empty\else
    \noindent\bibpreamble\par\nobreak
  \fi\fi
  \list{\@biblabel{\@arabic\c@enumiv}}%
  {\settowidth\labelwidth{\@biblabel{#1}}%
    \leftmargin\labelwidth
    \advance\leftmargin\labelsep
    \setlength{\itemsep}{0pt}
    \@openbib@code
    \usecounter{enumiv}%
    \let\p@enumiv\@empty
    \renewcommand*\theenumiv{\@arabic\c@enumiv}}%
  \sloppy\clubpenalty4000\widowpenalty4000%
  \sfcode`\.=\@m}

\AtBeginDocument{%
    \renewcommand*\bib@heading{\section*{\bibheading}}
}


%% LOAD BABEL
\RequirePackage[\sprache]{babel}[1997/01/23]

%% HYPERLINKS
\RequirePackage{url}
\newcommand{\urlprefix}{}
\@ifundefined{selectfont}{%
\def\UrlFont{\it}}
{\def\UrlFont{\rmfamily\itshape}
}


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% VARIABLES (ADJUST TO YOUR NEEDS!)
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

%% UNIVERSITY
\ifuzh@english
    \renewcommand*\uzh{University of Zurich}
\else
    \renewcommand*\uzh{Universit\"at Z\"urich}
\fi

%% UNIT (e.g. Institute)
\ifuzh@english
    \renewcommand*\uzhunit{University Unit}
\else
    \renewcommand*\uzhunit{Universit\"atseinheit}
\fi

\endinput
%%
%% End of file `uzhpub.cls'.
